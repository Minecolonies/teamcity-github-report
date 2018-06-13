<%@ include file="/include.jsp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<style type="text/css">
  p.sampleStyle {
    background: url("<c:url value='/plugins/samplePlugin/sample.gif'/>") no-repeat 0 2px;
  }

  #sidebar .blockAgents p.sampleStyle {
    margin-right: 5px;
    margin-left: 5px;
    padding-left: 1.9em;
  }
</style>
<c:url var="actionUrl" value="/viewPermissions.html"/>
<p class="sampleStyle">Sample Plugin</p>
<form action="${actionUrl}" class="clearfix">
    <input class="btn btn_mini submitButton" style="margin-right: 5px" id="search" type="submit" value="View My Permissions"/>
</form>
