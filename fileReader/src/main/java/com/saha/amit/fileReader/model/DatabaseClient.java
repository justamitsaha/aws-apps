package com.saha.amit.fileReader.model;

@Bean DatabaseClient databaseClient(ConnectionFactory connectionFactory){return DatabaseClient.create(connectionFactory);}
