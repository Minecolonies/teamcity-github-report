<c:choose><c:when test="${buildQueuePauser.queuePaused}"
    ><c:set var="buildQueuePauseActionText">Resume</c:set
    ></c:when
    ><c:otherwise
    ><c:set var="buildQueuePauseActionText">Pause Build Queue</c:set
    ></c:otherwise
    ></c:choose>
<authz:authorize allPermissions="ENABLE_DISABLE_AGENT">
  <jsp:attribute name="ifAccessDenied">
    <authz:authorize allPermissions="ENABLE_DISABLE_AGENT_FOR_PROJECT" poolId="${agent.agentType.agentPoolId}">
      <form action="${actionUrl}" id="queuePauserForm" method="post" class="clearfix">
        <input class="btn btn_mini submitButton" id="buildQueuePause" type="submit" value="${buildQueuePauseActionText}"/>
        <input type="hidden" name="newBuildQueuePausedState" value="${not buildQueuePauser.queuePaused}"/>
      </form>
    </authz:authorize>
  </jsp:attribute>
  <jsp:attribute name="ifAccessGranted">
    <form action="${actionUrl}" id="queuePauserForm" method="post" class="clearfix">
      <input class="btn btn_mini submitButton" id="buildQueuePause" type="submit" value="${buildQueuePauseActionText}"/>
      <input type="hidden" name="newBuildQueuePausedState" value="${not buildQueuePauser.queuePaused}"/>
    </form>
  </jsp:attribute>
</authz:authorize>
