-- liquibase formatted sql

--before execution
--create kzshop with password 'kzshopSecret';
--create database kzshop with owner kzshop;

--\connect chtracker
--CREATE EXTENSION IF NOT EXISTS timescaledb;

--in case if we will have to use UUIDs
--CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


--in order to install postgis
--https://postgis.net/docs/postgis_installation.html
--CREATE EXTENSION IF NOT EXISTS EXTENSION postgis;

-- if you built with raster support and want to install it --
--CREATE EXTENSION postgis_raster;

-- if you want to install topology support --
--CREATE EXTENSION postgis_topology;

-- if you built with sfcgal support and want to install it --
--CREATE EXTENSION postgis_sfcgal;

-- if you want to install tiger geocoder --
--CREATE EXTENSION fuzzystrmatch";
--CREATE EXTENSION postgis_tiger_geocoder;

-- if you installed with pcre
-- you should have address standardizer extension as well
--CREATE EXTENSION address_standardizer;

--CREATE EXTENSION IF NOT EXISTS timescaledb;

--in case if we will have to use UUIDs
--CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--in order to install postgis
--https://postgis.net/docs/postgis_installation.html
--CREATE EXTENSION IF NOT EXISTS 	postgis;

-- changeset yilativs:1 context:common failOnError:true splitStatements:false

create schema discord; --CATALOG, product, product_option

create table discord.message(
  	username text not null,
	gmtDateTime timestamp without time zone not null,
	avatarUrl text not null,
    messageText text,
    repliedText text,
    videoOriginalUrl text,
    videoPreviewUrl text,
    imageOriginalUrl text,
    imagePreviewUrl text,
    emojeTexts TEXT [],
    --todo consider storing emojes as two lists
    --emojes hstore
    PRIMARY KEY(username, gmtDateTime)
);
