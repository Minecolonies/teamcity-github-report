<%@ page import="jetbrains.buildServer.serverSide.statistics.build.BuildChartSettings" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="graphKey" value="${param['graphKey']}"/>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<c:set var="users" value="${buildGraphBean.settings.params['availableUsers']}"/>
<c:set var="selected" value="${buildGraphBean.settings.params['selectedUsers'][0]}"/>
<c:set var="cols" value="1"/>
<div class="agentsFilter" id="usersFilter${graphKey}">
  <div>
    <div>
      <a href="#" onclick="window.setBoxes('${graphKey}', false); return false" style="float: right; margin-left:10px;">None</a>
      <a href="#" onclick="window.setBoxes('${graphKey}', true); applyFilter('${graphKey}'); return false" style="float: right; margin-left:10px;">All</a>
      <strong>Show only build with changes of user</strong>
      <script type="text/javascript">
        window.setBoxes = function(key, val) {
          var boxes = document.forms[key+"Form"]['@filter.users'];
          for (var i = 0; i<boxes.length; i++) boxes[i].checked = val;
        }
      </script>
    </div>
    <div style="overflow-y:scroll; height:${graphBean.graphDescriptor.height-40}px; border:1px solid silver;">
    <table style="line-height:1em; border-collapse: collapse; " border="0">
      <c:forEach items="${users}" var="userName" varStatus="pos">
        <c:if test="${pos.index % cols == 0}"><tr></c:if>
        <td>
          <c:if test="${pos.index % cols != 0}">&nbsp;&nbsp;</c:if>
          <c:set var="id" value="${graphKey}userName${pos.index}"/>
          <c:set var="checked" value="${fn:contains(selected, userName)}"/>
          <forms:checkbox
            id="${id}" name="@filter.users" value="${userName}"
            checked="${checked}"
            onclick="applyFilter('${graphKey}');"
          /><label for="${id}" class="name"><c:out value="${userName}"/>
        </label>
        </td>
      </c:forEach>
    </table>
    </div>
  </div>
</div>