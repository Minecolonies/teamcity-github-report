<%@ include file="/include.jsp" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="View Permissions" scope="request"/>
<bs:page>
  <jsp:attribute name="head_include">
    <bs:linkCSS>
      /css/admin/adminMain.css
      /css/admin/projectConfig.css
      /css/admin/vcsRootsTable.css
    </bs:linkCSS>
    <bs:linkScript>
      /js/bs/blocks.js
      /js/bs/blocksWithHeader.js
      /js/bs/editProject.js
    </bs:linkScript>

    <script type="text/javascript">
      BS.Navigation.items = [
        {title:"Profile", url:'<c:url value="/profile.html"/>'},
        {title:"${pageTitle}", selected:true}
      ];
    </script>
  </jsp:attribute>

  <jsp:attribute name="body_include">
    <h2 class="noBorder">Global permissions</h2>
    <table class="runnerFormTable">
      <c:forEach items="${globalPermissions}" var="perm">
        <tr>
          <td>${perm}</td>
          <authz:authorize allPermissions="${perm}">
            <jsp:attribute name="ifAccessGranted">
            <td>+</td>
            </jsp:attribute>
            <jsp:attribute name="ifAccessDenied">
            <td>-</td>
            </jsp:attribute>
          </authz:authorize>
        </tr>
      </c:forEach>
    </table>

    <br/><br/>

    <h2 class="noBorder">Project permissions</h2>
    <table class="runnerFormTable">
      <tr>
        <td>&nbsp;</td>
        <c:forEach items="${projects}" var="proj">
          <th><c:out value="${proj.name}"/></th>
        </c:forEach>
      </tr>

      <c:forEach items="${projectPermissions}" var="perm">
        <tr>
          <td>${perm}</td>
          <c:forEach items="${projects}" var="proj">
            <authz:authorize projectId="${proj.projectId}" allPermissions="${perm}">
              <jsp:attribute name="ifAccessGranted">
              <td>+</td>
              </jsp:attribute>
              <jsp:attribute name="ifAccessDenied">
              <td>-</td>
              </jsp:attribute>
            </authz:authorize>
          </c:forEach>
        </tr>
      </c:forEach>
    </table>
  </jsp:attribute>
</bs:page>
