<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<l:settingsGroup title="Github PR Commenting">
    <tr>
        <th class="noBorder"><label for="username">Username:</label></th>
        <td>
            <props:textProperty name="usernameParameter" linkTitle="Username:" className="longField" cols="300" rows="1" expanded="true"/>
        </td>
    </tr>

    <tr>
        <th class="noBorder"><label for="password">Password:</label></th>
        <td>
            <props:passwordProperty name="passwordProperty" linkTitle="Password:" className="longField" cols="300" rows="1" expanded="true"/>
        </td>
    </tr>

    <tr>
        <th class="noBorder"><label for="token">API Token:</label></th>
        <td>
            <props:passwordProperty name="tokenProperty" linkTitle="API Token:" className="longField" cols="300" rows="1" expanded="true"/>
        </td>
    </tr>
</l:settingsGroup>