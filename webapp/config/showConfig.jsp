<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml"%>
<fmt:setBundle basename="org.cip4.elk.helk.messages.messages" />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<title><fmt:message key='app.name' /> - <fmt:message key='configuration' /></title>
<link rel="stylesheet" type="text/css" media="all" href="css/screen.css" />
</head>
<body>
	<div id="config">
	    <c:set var='config' value='${requestScope.config}'/>
        <c:set var='preprocessor' value='${requestScope.preprocessor}'/>
        <h1><fmt:message key='device.configuration'/></h1>
        <ul>
            <li>
                <label title="<fmt:message key='device.id.help'/>"><fmt:message key='device.id'/>:</label> ${config.ID}
            </li>
            <li>
                <label title="<fmt:message key='jdf.output.url.help'/>"><fmt:message key='jdf.output.url'/>:</label> <a href="${config.JDFOutputURL}">${config.JDFOutputURL}</a>
            </li>
            <li>
                <label title="<fmt:message key='local.jdf.output.url.help'/>"><fmt:message key='local.jdf.output.url'/>:</label> ${config.localJDFOutputURL}
            </li>
            <li>
                <label title="<fmt:message key='jdf.temp.url.help'/>"><fmt:message key='jdf.temp.url'/>:</label> ${config.JDFTempURL}
            </li>
            <li>
                <label title="<fmt:message key='jdf.validation'/>"><fmt:message key='jdf.validation'/>:</label>
                <c:choose>
                    <c:when test="${preprocessor.validation}">
                        <fmt:message key='jdf.validation.enabled'/> (<a href="<c:url value='/config?cmd=setValidation&validation=false'/>"><fmt:message key='jdf.validation.disable'/></a>)
                    </c:when>
                    <c:otherwise>
                        <fmt:message key='jdf.validation.disabled'/> (<a href="<c:url value='/config?cmd=setValidation&validation=true'/>"><fmt:message key='jdf.validation.enable'/></a>)
                    </c:otherwise>                    
                </c:choose>                
            </li>
            <%--
            <dt><fmt:message key='jmf.url'/>:</dt>
            <dd>${config.JMFURL}</dd>
            <dd><fmt:message key='jmf.url.help'/></dd>
            
            <dt><fmt:message key='url.schemes'/>:</dt>
            <dd>${config.URLSchemes}</dd>
            <dd><fmt:message key='url.schemes.help'/></dd>
            --%>
        </ul>
        <p>
            <a href="config/Device.xml"><fmt:message key='view.device.capabilities.file'/></a>
        </p>
     </div>
	
	
</body>
</html>
