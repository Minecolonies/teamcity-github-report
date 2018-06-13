<%@ include file="/include.jsp" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<bs:page>
  <jsp:attribute name="body_include">
    <h2>Hello <c:out value="${userName}"/>!</h2>

    <p>This example plugin demonstrates the following extensions:</p>

    <ul>
      <li>"Click me!" button is displayed on the Overview page</li>
      <li>When a build is run, extension-specific output is logged. The log can be seen on this page or on the build configuration's "Sample
        Extension" tab
      </li>
      <li>"Sample Build Result Tab" tab is shown on the build results page</li>
      <li>An additional chart is shown on the build configuration Statistics page</li>
      <li>"View My Permissions" button on the "My Settings & Tools" page</li>
      <li>"Pause Build Queue" button on the "Build Queue" page</li>
    </ul>

    Found log messages:
    <c:forEach items="${messages}" var="message">
      <br><span class="mono mono-12px"><c:out value="${message}"/></span>
    </c:forEach>
  </jsp:attribute>
</bs:page>
