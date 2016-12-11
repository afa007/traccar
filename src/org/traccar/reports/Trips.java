/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 Andrey Kunitsyn (andrey@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.reports;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jxls.area.Area;
import org.jxls.builder.xls.XlsCommentAreaBuilder;
import org.jxls.common.CellRef;
import org.jxls.formula.StandardFormulaProcessor;
import org.jxls.transform.Transformer;
import org.jxls.transform.poi.PoiTransformer;
import org.jxls.util.TransformerFactory;
import org.traccar.Context;
import org.traccar.mining.DataNode;
import org.traccar.mining.OutlierNodeDetect;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.reports.model.DeviceReport;
import org.traccar.reports.model.TripReport;

public final class Trips {

    private Trips() {
    }

    private static TripReport calculateTrip(List<Position> positions, int startIndex, int endIndex) {
        Position startTrip = positions.get(startIndex);
        Position endTrip = positions.get(endIndex);

        double speedMax = 0.0;
        double speedSum = 0.0;
        for (int i = startIndex; i <= endIndex; i++) {
            double speed = positions.get(i).getSpeed();
            speedSum += speed;
            if (speed > speedMax) {
                speedMax = speed;
            }
        }

        TripReport trip = new TripReport();

        long tripDuration = endTrip.getFixTime().getTime() - positions.get(startIndex).getFixTime().getTime();
        long deviceId = startTrip.getDeviceId();
        trip.setDeviceId(deviceId);
        trip.setDeviceName(Context.getIdentityManager().getDeviceById(deviceId).getName());

        trip.setStartPositionId(startTrip.getId());
        trip.setStartLat(startTrip.getLatitude());
        trip.setStartLon(startTrip.getLongitude());
        trip.setStartTime(startTrip.getFixTime());
        trip.setStartAddress(startTrip.getAddress());

        trip.setEndPositionId(endTrip.getId());
        trip.setEndLat(endTrip.getLatitude());
        trip.setEndLon(endTrip.getLongitude());
        trip.setEndTime(endTrip.getFixTime());
        trip.setEndAddress(endTrip.getAddress());

        boolean ignoreOdometer = Context.getDeviceManager()
                .lookupConfigBoolean(deviceId, "report.ignoreOdometer", false);
        trip.setDistance(ReportUtils.calculateDistance(startTrip, endTrip, !ignoreOdometer));
        trip.setDuration(tripDuration);
        trip.setAverageSpeed(speedSum / (endIndex - startIndex));
        trip.setMaxSpeed(speedMax);
        trip.setSpentFuel(ReportUtils.calculateFuel(startTrip, endTrip));

        return trip;
    }

    private static Collection<TripReport> detectTripsByTimeDistance(long deviceId, Date from, Date to) throws SQLException {

        ArrayList<Position> positions = new ArrayList<>(Context.getDataManager().getPositions(deviceId, from, to));

        return getTripsByBetween(positions);
    }


    public static Collection<TripReport> getTripsByBetween(List<Position> positions) {
        Collection<TripReport> result = new ArrayList<>();

        if (positions != null && !positions.isEmpty() && positions.size() >= 2) {

            int startIndex = 0, endIndex = 1;
            for (int i = 0; i < positions.size() - 1; i++) {

                double tripDuration = (positions.get(i + 1).getFixTime().getTime()
                        - positions.get(i).getFixTime().getTime()) / 1000;

                double tripDistance = (ReportUtils.calculateDistance(positions.get(i),
                        positions.get(i + 1), false));

                if (tripDuration > 30 * 60 && tripDistance > 1000 * 5) {

                    endIndex = i;
                    result.add(calculateTrip(positions, startIndex, endIndex));
                    startIndex = i + 1;
                }
                System.out.println("i:" + i + ", tripDuration:" + tripDuration + ", tripDistance:" + tripDistance);
            }

        }

        return result;
    }

    public static Collection<TripReport> getTrips(List<Position> positions) {
        Collection<TripReport> result = new ArrayList<>();

        if (positions != null && !positions.isEmpty() && positions.size() >= 2) {

            List<Double> durationList = new ArrayList<>();
            List<Double> distanceList = new ArrayList<>();

            for (int i = 0; i < positions.size() - 1; i++) {
                double tripDuration = (positions.get(i + 1).getFixTime().getTime()
                        - positions.get(i).getFixTime().getTime()) / 1000;
                double tripDistance = (ReportUtils.calculateDistance(positions.get(i),
                        positions.get(i + 1), false));
//                if (tripDuration == 0.0 && tripDistance == 0.0) {
//                    continue;
//                }
                durationList.add(tripDuration);
                distanceList.add(tripDistance);
                System.out.println("i:" + i + ", tripDuration:" + tripDuration + ", tripDistance:" + tripDistance);
            }

            // 检测离异点
            OutlierNodeDetect lof = new OutlierNodeDetect();
            List<DataNode> nodeList = lof.detect2Dimention(durationList, distanceList);
            for (DataNode node : nodeList) {
                if (node.getLof() > 1) {
                    durationList.set(Integer.parseInt(node.getNodeName()), null);
                }
            }

//            int startIndex = 0;
//            int endIndex = 0;
//            for (int i = 0; i < durationList.size(); i++) {
//                if (durationList.get(i) == null) {
////                    result.add(calculateTrip(positions, startIndex, endIndex));
//                    startIndex = endIndex = i + 1;
//                } else {
//                    endIndex++;
//                }
//            }
//            if (startIndex != endIndex) {
////                result.add(calculateTrip(positions, startIndex, endIndex));
//            }
        }

        return result;
    }

    private static Collection<TripReport> detectTrips(long deviceId, Date from, Date to) throws SQLException {
        double speedThreshold = Context.getConfig().getDouble("event.motion.speedThreshold", 0.01);
        long minimalTripDuration = Context.getConfig().getLong("report.trip.minimalTripDuration", 300) * 1000;
        double minimalTripDistance = Context.getConfig().getLong("report.trip.minimalTripDistance", 500);
        long minimalParkingDuration = Context.getConfig().getLong("report.trip.minimalParkingDuration", 300) * 1000;
        boolean greedyParking = Context.getConfig().getBoolean("report.trip.greedyParking");
        Collection<TripReport> result = new ArrayList<>();

        ArrayList<Position> positions = new ArrayList<>(Context.getDataManager().getPositions(deviceId, from, to));
        if (positions != null && !positions.isEmpty()) {
            int previousStartParkingIndex = 0;
            int startParkingIndex = -1;
            int previousEndParkingIndex = 0;
            int endParkingIndex = 0;

            boolean isMoving = false;
            boolean isLast = false;
            boolean skipped = false;
            boolean tripFiltered = false;

            for (int i = 0; i < positions.size(); i++) {
                isMoving = positions.get(i).getSpeed() > speedThreshold;
                isLast = i == positions.size() - 1;

                if ((isMoving || isLast) && startParkingIndex != -1) {
                    if (!skipped || previousEndParkingIndex == 0) {
                        previousEndParkingIndex = endParkingIndex;
                    }
                    endParkingIndex = i;
                }
                if (!isMoving && startParkingIndex == -1) {
                    if (greedyParking) {
                        long tripDuration = positions.get(i).getFixTime().getTime()
                                - positions.get(endParkingIndex).getFixTime().getTime();
                        double tripDistance = ReportUtils.calculateDistance(positions.get(endParkingIndex),
                                positions.get(i), false);
                        tripFiltered = tripDuration < minimalTripDuration && tripDistance < minimalTripDistance;
                        if (tripFiltered) {
                            startParkingIndex = previousStartParkingIndex;
                            endParkingIndex = previousEndParkingIndex;
                            tripFiltered = false;
                        } else {
                            previousStartParkingIndex = i;
                            startParkingIndex = i;
                        }
                    } else {
                        long tripDuration = positions.get(i).getFixTime().getTime()
                                - positions.get(previousEndParkingIndex).getFixTime().getTime();
                        double tripDistance = ReportUtils.calculateDistance(positions.get(previousEndParkingIndex),
                                positions.get(i), false);
                        tripFiltered = tripDuration < minimalTripDuration && tripDistance < minimalTripDistance;
                        startParkingIndex = i;
                    }
                }
                if (startParkingIndex != -1 && (endParkingIndex > startParkingIndex || isLast)) {
                    long parkingDuration = positions.get(endParkingIndex).getFixTime().getTime()
                            - positions.get(startParkingIndex).getFixTime().getTime();
                    if ((parkingDuration >= minimalParkingDuration || isLast)
                            && previousEndParkingIndex < startParkingIndex) {
                        if (!tripFiltered) {
                            result.add(calculateTrip(positions, previousEndParkingIndex, startParkingIndex));
                        }
                        previousEndParkingIndex = endParkingIndex;
                        skipped = false;
                    } else {
                        skipped = true;
                    }
                    startParkingIndex = -1;
                }
            }
        }
        return result;
    }

    public static Collection<TripReport> getObjects(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
                                                    Date from, Date to) throws SQLException {
        ArrayList<TripReport> result = new ArrayList<>();
        for (long deviceId : ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
//            result.addAll(detectTrips(deviceId, from, to));
            result.addAll(detectTripsByTimeDistance(deviceId, from, to));
        }
        return result;
    }

    public static void getExcel(OutputStream outputStream,
                                long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
                                Date from, Date to) throws SQLException, IOException {
        ArrayList<DeviceReport> devicesTrips = new ArrayList<>();
        ArrayList<String> sheetNames = new ArrayList<>();
        for (long deviceId : ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<TripReport> trips = detectTrips(deviceId, from, to);
            DeviceReport deviceTrips = new DeviceReport();
            Device device = Context.getIdentityManager().getDeviceById(deviceId);
            deviceTrips.setDeviceName(device.getName());
            sheetNames.add(deviceTrips.getDeviceName());
            if (device.getGroupId() != 0) {
                Group group = Context.getDeviceManager().getGroupById(device.getGroupId());
                if (group != null) {
                    deviceTrips.setGroupName(group.getName());
                }
            }
            deviceTrips.setObjects(trips);
            devicesTrips.add(deviceTrips);
        }
        String templatePath = Context.getConfig().getString("report.templatesPath",
                "templates/export/");
        try (InputStream inputStream = new FileInputStream(templatePath + "/trips.xlsx")) {
            org.jxls.common.Context jxlsContext = PoiTransformer.createInitialContext();
            jxlsContext.putVar("devices", devicesTrips);
            jxlsContext.putVar("sheetNames", sheetNames);
            jxlsContext.putVar("from", from);
            jxlsContext.putVar("to", to);
            jxlsContext.putVar("distanceUnit", ReportUtils.getDistanceUnit(userId));
            jxlsContext.putVar("speedUnit", ReportUtils.getSpeedUnit(userId));
            Transformer transformer = TransformerFactory.createTransformer(inputStream, outputStream);
            List<Area> xlsAreas = new XlsCommentAreaBuilder(transformer).build();
            for (Area xlsArea : xlsAreas) {
                xlsArea.applyAt(new CellRef(xlsArea.getStartCellRef().getCellName()), jxlsContext);
                xlsArea.setFormulaProcessor(new StandardFormulaProcessor());
                xlsArea.processFormulas();
            }
            transformer.deleteSheet(xlsAreas.get(0).getStartCellRef().getSheetName());
            transformer.write();
        }
    }
}
