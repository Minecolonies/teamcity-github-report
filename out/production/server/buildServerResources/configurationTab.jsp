<%@ include file="/include.jsp" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div style="margin-top: 1em">
  Sample configuration tab content

  <ul>
  <c:forEach items="${messages}" var="message">
    <li><c:out value="${message}"/></li>
  </c:forEach>
  </ul>
</div>
