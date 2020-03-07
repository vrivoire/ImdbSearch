module com.vrivoire.imdbsearch {
    requires java.desktop;
    requires java.sql;
    requires themoviedbapi;
    requires jackson.core;
    requires jackson.annotations;
    requires jackson.databind;
    requires slf4j.api;
    requires api.common;
    requires httpclient;
    requires httpcore;
    requires commons.lang3;
    requires API.OMDB;
    requires commons.codec;
    requires org.apache.commons.collections;
    requires org.apache.commons.io;
    requires commons.collections;
    requires commons.beanutils;
    requires commons.logging;
    requires log4j;

    exports com.vrivoire.imdbsearch;
}
