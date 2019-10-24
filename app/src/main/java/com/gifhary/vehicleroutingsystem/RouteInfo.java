package com.gifhary.vehicleroutingsystem;

public class RouteInfo {
    private String routeName;
    private String routeID;
    private String distance;
    private String duration;
    private String durationInTraffic;

    public RouteInfo(String routeName, String routeID, String distance, String duration, String durationInTraffic) {
        this.routeName = routeName;
        this.routeID = routeID;
        this.distance = distance;
        this.duration = duration;
        this.durationInTraffic = durationInTraffic;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getRouteID() {
        return routeID;
    }

    public void setRouteID(String routeID) {
        this.routeID = routeID;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDurationInTraffic() {
        return durationInTraffic;
    }

    public void setDurationInTraffic(String durationInTraffic) {
        this.durationInTraffic = durationInTraffic;
    }
}
