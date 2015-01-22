<!DOCTYPE html>
<%@page import="java.net.URL"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
    
    <script>
    // Polymer will include a bunch of scripts to shim HTMLImports...
    // so we test for native support before including that polyfill
    if (!('import' in document.createElement('link'))) {
    	 document.write('<script type="text/javascript" src="/polymer/webcomponentsjs/src/HTMLImports/HTMLImports.js" ></scr'+'ipt>');
      }
    </script>
    <!-- 
    Here we include a vulcanized subset of required polymer elements.
    This is so we don't have to download a ton of files to startup the compiler
     -->
    <link rel="import" href="/xapi-polymer.min.html">

    <!-- Polyfill EventSource for older IE versions -->
	<script type="text/javascript" src="/eventsource.min.js" ></script>
    <!-- 
    Include our compiled app, pointing to the super dev mode server,
    if sdm is running; otherwise, just use the compiled output.
    -->
	<script type="text/javascript" src="<% 
        try {
          URL url = new URL("http://localhost:9876");
          url.openConnection();
          out.println("http://localhost:9876");     
	    } catch (Exception ignored) {}
      %>/Gwtc/Gwtc.nocache.js" ></script>
</head>
<body>
  <xapi-gwtc
    moduleName="net.wetheinter.gwtc.Gwtc"
    jsinteropmode="JS" 
    sourceLevel="1.8"
  />
</body>
</html>
