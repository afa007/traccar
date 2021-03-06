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

import org.json.JSONArray;
import org.json.JSONObject;
import org.traccar.Context;
import org.traccar.helper.DateUtil;
import org.traccar.helper.Log;
import org.traccar.model.Position;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public final class ReportUtils {

    private ReportUtils() {
    }

    public static String getDistanceUnit(long userId) {
        String unit = Context.getPermissionsManager().getUser(userId).getDistanceUnit();
        if (unit == null) {
            unit = Context.getPermissionsManager().getServer().getDistanceUnit();
        }
        return unit != null ? unit : "km";
    }

    public static String getSpeedUnit(long userId) {
        String unit = Context.getPermissionsManager().getUser(userId).getSpeedUnit();
        if (unit == null) {
            unit = Context.getPermissionsManager().getServer().getSpeedUnit();
        }
        return unit != null ? unit : "kn";
    }

    public static Collection<Long> getDeviceList(Collection<Long> deviceIds, Collection<Long> groupIds) {
        Collection<Long> result = new ArrayList<>();
        result.addAll(deviceIds);
        for (long groupId : groupIds) {
            result.addAll(Context.getPermissionsManager().getGroupDevices(groupId));
        }
        return result;
    }

    public static double calculateDistance(Position firstPosition, Position lastPosition) {
        return calculateDistance(firstPosition, lastPosition, true);
    }

    public static double calculateDistance(Position firstPosition, Position lastPosition, boolean useOdometer) {
        double distance = 0.0;
        double firstOdometer = 0.0;
        double lastOdometer = 0.0;
        if (firstPosition.getAttributes().containsKey(Position.KEY_ODOMETER)) {
            firstOdometer = ((Number) firstPosition.getAttributes().get(Position.KEY_ODOMETER)).doubleValue();
        }
        if (lastPosition.getAttributes().containsKey(Position.KEY_ODOMETER)) {
            lastOdometer = ((Number) lastPosition.getAttributes().get(Position.KEY_ODOMETER)).doubleValue();
        }
        if (useOdometer && (firstOdometer != 0.0 || lastOdometer != 0.0)) {
            distance = lastOdometer - firstOdometer;
        } else if (firstPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)
                && lastPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)) {
            distance = ((Number) lastPosition.getAttributes().get(Position.KEY_TOTAL_DISTANCE)).doubleValue()
                    - ((Number) firstPosition.getAttributes().get(Position.KEY_TOTAL_DISTANCE)).doubleValue();
        }
        return distance;
    }

    public static String calculateFuel(Position firstPosition, Position lastPosition) {

        if (firstPosition.getAttributes().get(Position.KEY_FUEL) != null
                && lastPosition.getAttributes().get(Position.KEY_FUEL) != null) {
            try {
                switch (firstPosition.getProtocol()) {
                    case "meitrack":
                    case "galileo":
                    case "noran":
                        BigDecimal v = new BigDecimal(firstPosition.getAttributes().get(Position.KEY_FUEL).toString());
                        v = v.subtract(new BigDecimal(lastPosition.getAttributes().get(Position.KEY_FUEL).toString()));
                        return v.setScale(2, RoundingMode.HALF_EVEN).toString() + " %";
                    default:
                        break;
                }
            } catch (Exception error) {
                Log.warning(error);
            }
        }
        return "-";
    }

    public static List<Position> getPositionList(String json) {

        List<Position> list = new ArrayList<>();

        JSONArray positions = new JSONArray(json);
        double lastLatitude = 0.0;
        double lastLongitude = 0.0;
        Date lastFixTime = new Date();
        for (int i = 0; i < positions.length(); i++) {

            Position pos = new Position();

            JSONObject info = positions.getJSONObject(i);

            // id
            pos.setId(info.getInt("id"));
            pos.setDeviceId(info.getLong("deviceId"));
            pos.setFixTime(DateUtil.parseDate(info.getString("fixTime")));
            pos.setLatitude(info.getDouble("latitude"));
            pos.setLongitude(info.getDouble("longitude"));

            JSONObject attributes = info.getJSONObject("attributes");
            pos.set(Position.KEY_TOTAL_DISTANCE, attributes.getDouble("totalDistance"));

            if (pos.getFixTime().compareTo(lastFixTime) != 0) {
                list.add(pos);
                lastLatitude = pos.getLatitude();
                lastLongitude = pos.getLongitude();
                lastFixTime = pos.getFixTime();
            }
        }

        return list;
    }

}
