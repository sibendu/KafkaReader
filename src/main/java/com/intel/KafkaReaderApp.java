package com.intel;

import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KafkaReaderApp implements CommandLineRunner { //// ApplicationRunner { // {

	private static Logger LOG = LoggerFactory.getLogger(KafkaReaderApp.class);

	@Autowired
	KafkaConsumerClient client;

//	@Autowired
//	public IntelObjectRepository intelObjRepo;

	public static void main(String[] args) {

		LOG.info("STARTING THE APPLICATION");
		SpringApplication.run(KafkaReaderApp.class, args);
		LOG.info("APPLICATION FINISHED");
	}

	@Override
	public void run(String... args) {

		
		LOG.info("EXECUTING : command line runner");

		client.consume();

		LOG.info("EXECUTING : command line runner reached end");

	}

	/*
	 * @Override public void run(ApplicationArguments arg0) throws Exception {
	 * System.out.println("Hello World from Application Runner");
	 * 
	 * LOG.info("EXECUTING : command line runner");
	 * 
	 * KafkaConsumerClient client = new KafkaConsumerClient();
	 * client.processMessage("");
	 * 
	 * LOG.info("EXECUTING : command line runner reached end"); }
	 */

}
