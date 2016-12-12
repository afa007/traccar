package org.traccar.mining;

import java.util.ArrayList;
import java.util.List;

public class DataNode {

    // 样本点名
    private String nodeName;

    // 样本点的维度
    private double[] dimensioin;

    // k-距离
    private double kDistance;

    // k-领域
    private List<DataNode> kNeighbor = new ArrayList<DataNode>();

    // 到给定点的欧几里得距离
    private double distance;

    // 可达密度
    private double reachDensity;

    // 可达距离
    private double reachDis;

    // 局部离群因子
    private double lof;

    public DataNode() {

    }

    public DataNode(String nodeName, double[] dimensioin) {
        this.nodeName = nodeName;
        this.dimensioin = dimensioin;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public double[] getDimensioin() {
        return dimensioin;
    }

    public void setDimensioin(double[] dimensioin) {
        this.dimensioin = dimensioin;
    }

    public double getkDistance() {
        return kDistance;
    }

    public void setkDistance(double kDistance) {
        this.kDistance = kDistance;
    }

    public List<DataNode> getkNeighbor() {
        return kNeighbor;
    }

    public void setkNeighbor(List<DataNode> kNeighbor) {
        this.kNeighbor = kNeighbor;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getReachDensity() {
        return reachDensity;
    }

    public void setReachDensity(double reachDensity) {
        this.reachDensity = reachDensity;
    }

    public double getReachDis() {
        return reachDis;
    }

    public void setReachDis(double reachDis) {
        this.reachDis = reachDis;
    }

    public double getLof() {
        return lof;
    }

    public void setLof(double lof) {
        this.lof = lof;
    }

}
