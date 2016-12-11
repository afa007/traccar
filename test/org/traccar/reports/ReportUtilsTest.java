package org.traccar.reports;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.helper.DateUtil;
import org.traccar.helper.FileHelper;
import org.traccar.model.Position;
import org.traccar.reports.model.TripReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReportUtilsTest {

    @Test
    public void testCalculateDistance() {
        Position startPosition = new Position();
        startPosition.set(Position.KEY_TOTAL_DISTANCE, 500.0);
        Position endPosition = new Position();
        endPosition.set(Position.KEY_TOTAL_DISTANCE, 700.0);
        Assert.assertEquals(ReportUtils.calculateDistance(startPosition, endPosition), 200.0, 10);
        startPosition.set(Position.KEY_ODOMETER, 50000);
        endPosition.set(Position.KEY_ODOMETER, 51000);
        Assert.assertEquals(ReportUtils.calculateDistance(startPosition, endPosition), 1000.0, 10);
    }

    @Test
    public void testCalculateSpentFuel() {
        Position startPosition = new Position();
        Position endPosition = new Position();
        Assert.assertEquals(ReportUtils.calculateFuel(startPosition, endPosition), "-");
        startPosition.setProtocol("meitrack");
        startPosition.set(Position.KEY_FUEL, 0.07);
        endPosition.set(Position.KEY_FUEL, 0.05);
        Assert.assertEquals(ReportUtils.calculateFuel(startPosition, endPosition), "0.02 %");
        startPosition.setProtocol("galileo");
        Assert.assertEquals(ReportUtils.calculateFuel(startPosition, endPosition), "0.02 %");
        startPosition.setProtocol("noran");
        Assert.assertEquals(ReportUtils.calculateFuel(startPosition, endPosition), "0.02 %");
    }

    @Test
    public void testGetTrips() {
        ArrayList<Position> positions = new ArrayList<>();
        Position p = new Position();
        p.setLatitude(40.04501135);
        p.setLongitude(116.40915571);
        p.setFixTime(DateUtil.parseDate("2016-12-08T11:47:20.000+0000"));
        p.set(Position.KEY_TOTAL_DISTANCE, 8405.36);
        positions.add(p);

        p = new Position();
        p.setLatitude(40.04503711);
        p.setLongitude(116.40913201);
        p.setFixTime(DateUtil.parseDate("2016-12-08T11:47:21.000+0000"));
        p.set(Position.KEY_TOTAL_DISTANCE, 8408.87);
        positions.add(p);

        p = new Position();
        p.setLatitude(40.04502872);
        p.setLongitude(116.40908614);
        p.setFixTime(DateUtil.parseDate("2016-12-08T11:47:23.000+0000"));
        p.set(Position.KEY_TOTAL_DISTANCE, 8412.95);
        positions.add(p);

        p = new Position();
        p.setLatitude(40.04499785);
        p.setLongitude(116.40908098);
        p.setFixTime(DateUtil.parseDate("2016-12-08T11:47:29.000+0000"));
        p.set(Position.KEY_TOTAL_DISTANCE, 8422.37);
        positions.add(p);

        p = new Position();
        p.setLatitude(40.04498106);
        p.setLongitude(116.40908823);
        p.setFixTime(DateUtil.parseDate("2016-12-08T11:47:31.000+0000"));
        p.set(Position.KEY_TOTAL_DISTANCE, 8428.71);
        positions.add(p);

        p = new Position();
        p.setLatitude(40.04498338);
        p.setLongitude(116.40908698);
        p.setFixTime(DateUtil.parseDate("2016-12-08T11:47:32.000+0000"));
        p.set(Position.KEY_TOTAL_DISTANCE, 8428.99);
        positions.add(p);

        p = new Position();
        p.setLatitude(40.04493377);
        p.setLongitude(116.40904806);
        p.setFixTime(DateUtil.parseDate("2016-12-08T11:47:35.000+0000"));
        p.set(Position.KEY_TOTAL_DISTANCE, 8435.47);
        positions.add(p);

        p = new Position();
        p.setLatitude(40.04492015);
        p.setLongitude(116.40896953);
        p.setFixTime(DateUtil.parseDate("2016-12-08T11:47:40.000+0000"));
        p.set(Position.KEY_TOTAL_DISTANCE, 8442.96);
        positions.add(p);


        Collection<TripReport> result = Trips.getTrips(positions);
        Assert.assertEquals(result.size(), 1);
    }

    @Test
    public void testGetTripsJsonFile() {

        FileHelper helper = new FileHelper();
        String json = helper.ReadFile("D:\\json.txt");

        List<Position> positions = ReportUtils.getPositionList(json);
        Collection<TripReport> result = Trips.getTripsByBetween(positions);
        Assert.assertEquals(result.size(), 1);
    }

    @Test
    public void testJsonArray() {
        FileHelper helper = new FileHelper();
        String json = helper.ReadFile("D:\\json.txt");

        System.out.println(json);

        List<Position> list = ReportUtils.getPositionList(json);
        System.out.println(list.size());
    }
}
