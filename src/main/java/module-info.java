module com.k4j.lpg {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires java.sql;
    requires org.slf4j;
    requires com.google.gson;
    requires java.net.http;
    requires java.prefs;
    requires ch.qos.logback.classic;

    opens com.k4j.lpg to javafx.fxml;
    opens com.k4j.lpg.controllers to javafx.fxml;
    opens com.k4j.lpg.models to com.google.gson;
    
    exports com.k4j.lpg;
    exports com.k4j.lpg.controllers;
    exports com.k4j.lpg.models;
    exports com.k4j.lpg.services;
    exports com.k4j.lpg.utils;
}