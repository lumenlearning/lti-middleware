package net.unicon.lti.model;

import java.util.Objects;

public class ResyncContextDeployment {
    private String ltiContextId;
    private String iss;
    private String clientId;
    private String deploymentId;

    public ResyncContextDeployment() {}

    public String getLtiContextId() {
        return ltiContextId;
    }

    public void setLtiContextId(String ltiContextId) {
        this.ltiContextId = ltiContextId;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResyncContextDeployment that = (ResyncContextDeployment) o;
        return ltiContextId.equals(that.ltiContextId) && iss.equals(that.iss) && clientId.equals(that.clientId) && deploymentId.equals(that.deploymentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ltiContextId, iss, clientId, deploymentId);
    }

    @Override
    public String toString() {
        return "ResyncContextDeployment{" +
                "ltiContexttId='" + ltiContextId + '\'' +
                ", iss='" + iss + '\'' +
                ", clientId='" + clientId + '\'' +
                ", deploymentId='" + deploymentId + '\'' +
                '}';
    }
}
