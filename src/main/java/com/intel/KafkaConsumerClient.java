package com.intel;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.intel.model.IntelDAO;
import com.intel.model.IntelMetric;
import com.intel.model.IntelMetricRepository;
import com.intel.model.IntelObject;
import com.intel.model.IntelObjectRepository;

@Configuration
public class KafkaConsumerClient {

	private KafkaConsumer<String, String> kafkaConsumer;
	private String bootstrapServer = "";
	private int count;
	private String topicName = "";
	private String consumerGroup = "";
	private String username = "";
	private String password = "";
	private String jksFilePath = "";

	private String trustStorePassword = "+ru$tSt0r3";
	private String saslProtocol = "SASL_SSL";
	private String saslMechanism = "SCRAM-SHA-512";
	private String serviceName = "kafka";
	private String startingOffset = "latest";
	private String commitInterval = "5000";
	private String allowAutoCommit = "true";
	private Integer pollingTimeout = Integer.valueOf(5000);

	private String env;
	private String outputFile;
	private FileOutputStream fos;

	private String NEW_LINE = "\n";
	
	private static String[] metricsToWatch = new String[] {"currentSNR","usAvgCn0","latency","txPower","uptime","dvbS2Crc8Error","dsAllocatedBw","usAllocatedBw"};
	
	@Autowired
	public IntelObjectRepository intelObjRepo;

	@Autowired
	public IntelMetricRepository intelMetricRepo;

	public KafkaConsumerClient() {

		initialize();

		construct();
	}

	public boolean processMessage(String message) {
		boolean processed = false;
		
		try {
			// message =
			// "{\"object_id\":3013275,\"objectType\":\"sspc\",\"timestamp\":\"2022-06-22T12:42:00Z\",\"sspcName\":\"LQD-IQDESKTOP-46556-GS
			// :
			// LQD_FLEX_SILVER\",\"terminalId\":\"2987811\",\"terminalName\":\"LQD-IQDESKTOP-46556-GS\",\"metrics\":{\"dsCirDemandxx\":1900.56},\"resolution\":30,\"streamType\":\"stats\",\"inetName\":\"FLX.FUS.IS33.C4L1.025-K60H1.000.N1:21502\",\"siteName\":\"FUS-IS33-SITE-FLX\",\"satelliteName\":\"IS33E[4733]\",\"spacecraftName\":\"IS33E\",\"teleportName\":\"FUS\",\"beamName\":\"FUS-IS33-C4L-K60H\",\"serviceArea\":\"SA_2\",\"customerName\":\"LQD\",\"serialNumber\":\"46555\",\"terminalType\":\"LQD-IQDESKTOP-3W-FIXED-10108\",\"mobilityType\":\"FIXED\",\"terminalModel\":\"IQDESKTOP\"}";

			JSONObject msg = new JSONObject(message);

			String serialNumber = msg.get("serialNumber").toString();

			String objectType = msg.get("objectType").toString();
			String objectId = msg.get("object_id").toString();
			String terminalName = msg.get("terminalName").toString();
			String inetName = msg.get("inetName").toString();
			String siteName = msg.get("siteName").toString();
			String satelliteName = msg.get("satelliteName").toString();
			String spaceCraftName = msg.get("spacecraftName").toString();
			String teleportName = msg.get("teleportName").toString();
			String beamName = msg.get("beamName").toString();
			String serviceArea = msg.get("serviceArea").toString();
			String customerName = msg.get("customerName").toString();

			String terminalType = msg.get("terminalType").toString();
			String mobilityType = msg.get("mobilityType").toString();
			String terminalModel = msg.get("terminalModel").toString();
			String streamType = msg.get("streamType").toString();
			String resolution = msg.get("resolution").toString();
			String timestamp = msg.get("timestamp").toString();

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			// sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			Date dateTime = sdf.parse(timestamp);
			// System.out.println("Parsed: " + dateTime);

			JSONObject metrics = (JSONObject) msg.get("metrics");

			String metricName = null;
			Double metricValue = null;

			String x = "\"metrics\":{\"";
			String y = message.substring(message.indexOf(x) + x.length(), message.length());
			metricName = y.substring(0, y.indexOf("\""));
			// System.out.println(metricName);

			
			for (int i = 0; i < metricsToWatch.length; i++) {
				if (metricName.equals(metricsToWatch[i])) {
					metricValue = new Double(metrics.get(metricName).toString());
					break;
				} 	
			}	
			
			if(metricValue != null) {
				System.out.println("Processing message for Seria# "+serialNumber + " : " + metricName + " = " + metricValue);
				
			}else {	
				System.out.println("Metric " + metricName + " , ignoring message ...");
				return processed;
			}

			IntelObject iObj = intelObjRepo.findBySerialNo(serialNumber);
			IntelMetric metric = null;

			if (iObj != null) {
				// System.out.println("SerialNumber existing");

				metric = new IntelMetric(dateTime, metricName, metricValue, iObj);

				metric = intelMetricRepo.save(metric);

				System.out.println("Existing serialNo, Metric saved ...");

			} else {
				// System.out.println("New SerialNumber");
				iObj = new IntelObject(serialNumber, objectId, objectType);
				metric = new IntelMetric(dateTime, metricName, metricValue, iObj);

				iObj.addMetric(metric);

				iObj = intelObjRepo.save(iObj);

				System.out.println("New SerialNo & Metric saved");
			}

			processed = true;

		} catch (Exception e) {
			System.out.println("Error processing message: " + e.getMessage());
			// e.printStackTrace();
		}
		return processed;
	}

	public void consume() {
		boolean gotData = false;
		int totalMessagesPrinted = 0;

		System.out.println("6. Starting to consume data");
		System.out.println("");

		try {
			while (true) {
				ConsumerRecords<String, String> messages = this.kafkaConsumer
						.poll(Duration.ofMillis(this.pollingTimeout.intValue()));

				if (messages != null) {

					System.out.println("Polled. Message count = " + messages.count());

					for (ConsumerRecord<String, String> message : messages) {

						if (!messages.isEmpty()) {
							gotData = true;

							String thisMessage = (String) message.value();
							
							//System.out.println("Message recvd: "+thisMessage);
							boolean processed = processMessage(thisMessage);
							
							/*
							if (!processed) {
								System.out.println("Message process & saved to database ...");
							} else {
								fos.write(((String) message.value()).getBytes());
								fos.write(NEW_LINE.getBytes());
								fos.write(NEW_LINE.getBytes());
								
								System.out.println("---------------");
								System.out.println("Message ignored ...");
								System.out.println("---------------");

								//totalMessagesPrinted++;
							}*/
							System.out.println("Messages batch processed ... Polling again ....");
						}

						this.kafkaConsumer.commitAsync();
					}

//					if (totalMessagesPrinted > 10) {
//						break;
//					}
				} else {
					System.out.println("No message after polling ... Polling again ...");
				}

//				if (gotData) {
//					System.out.println("7. Consuming data from topic " + this.topicName + " -> SUCCESS");
//					endProgram("Received and processed data");
//				} else {
//					System.out.println("7. Consuming data from topic " + this.topicName + " -> FAILURE");
//					endProgram("No data found in topic");
//				}
			}//end of while
		} catch (Exception e) {
			System.out.println("7. Consuming data from topic " + this.topicName + " -> FAILURE. Terminating run.....");
			endProgram(e.getMessage());
		} finally {
			try {
				this.kafkaConsumer.close();
				this.fos.close();
			} catch (Exception e) {
				System.out.println("Error in finalzation: " + e.getMessage());
			}
		}
	}

	private void initialize() {

		try {
			this.env = "PROD";// System.getProperty("INTSAT_ENV");

			if (env.trim().equalsIgnoreCase("PROD")) {
				this.bootstrapServer = "metrics.intelsat.com:9093";// ob.readLine();
				this.topicName = "metrics-dsd-terminals-30";// ob.readLine();
				this.consumerGroup = "grp-user-dsd-stats";// ob.readLine();
				this.password = "yjp3rxbe4Pjh";
				this.username = "user-dsd-stats";// ob.readLine();
			} else {
				this.bootstrapServer = "metrics.intsat.com:9093";// ob.readLine();
				this.topicName = "metrics-dsd-terminals-30";// ob.readLine();
				this.consumerGroup = "grp-user-dsd-stats";// ob.readLine();
				this.password = "pF7n3DBneBOq";
				this.username = "user-dsd-stats";// ob.readLine();
			}

			if (env.trim().equalsIgnoreCase("LOCAL")) {
				this.jksFilePath = "C:\\Temp\\FastAPI\\Kafka\\extract_dev-src\\extract_dev\\";// System.getProperty("KEYSTORE_PATH");
				this.outputFile = "C:\\Temp\\FastAPI\\Kafka\\messages.json";
			} else {
				this.jksFilePath = "/home/sibendu/client/";// System.getProperty("KEYSTORE_PATH");
				this.outputFile = "/home/sibendu/KafkaReader/messages.json";
			}

			this.fos = new FileOutputStream(new File(outputFile));

		} catch (Exception e) {
			endProgram("Error in initializaiton: " + e.getMessage());
		}

		String kafkaServerAddress = this.bootstrapServer.replace(":9093", "");
		InetAddress[] dnsResult = null;

		try {
			dnsResult = InetAddress.getAllByName(kafkaServerAddress);

			for (InetAddress inetAddress : dnsResult) {
				System.out.println("1. Resolving host " + inetAddress + " -> SUCCESS");
			}
		} catch (UnknownHostException e) {
			System.out.println("1. Resolving host " + kafkaServerAddress + " -> FAILURE");
			endProgram("unable to resolve address: " + this.bootstrapServer);
			System.exit(1);
		}

		if (dnsResult == null) {
			System.out.println("1. Resolving host " + kafkaServerAddress + " -> FAILURE");
			endProgram("DNS result is null");
			System.exit(1);
		}

		try {
			InetAddress ipAddress = InetAddress.getByName(dnsResult[0].getHostAddress());
			System.out.println("2. Retrieving IP address for " + ipAddress + " -> SUCCESS");
		} catch (UnknownHostException e) {
			System.out.println("2. Retrieving IP address for " + kafkaServerAddress + " -> FAILURE");
			endProgram(e.getCause().getMessage());
			System.exit(1);
		}

		System.out.println("3. Connecting to Intelsat Kafka cluster -> SUCCESS");

		/*
		 * BufferedReader ob = new BufferedReader(new InputStreamReader(System.in)); try
		 * { System.out.print("Enter the URL for the bootstrap server: ");
		 * this.bootstrapServer = "metrics.intsat.com:9093";// ob.readLine();
		 * System.out.print("Enter the URL for the bootstrap server: " +
		 * this.bootstrapServer); } catch (Exception e) {
		 * System.out.println("ERROR - failed to read input string");
		 * endProgram("Invalid input specified"); System.exit(1); }
		 */
	}

	private void construct() {
		Properties props = new Properties();

		props.put("security.protocol", this.saslProtocol);
		props.put("bootstrap.servers", this.bootstrapServer);
		props.put("group.id", this.consumerGroup);
		props.put("enable.auto.commit", this.allowAutoCommit);
		props.put("auto.commit.interval.ms", this.commitInterval);
		props.put("auto.offset.reset", this.startingOffset);
		props.put("key.deserializer", StringDeserializer.class.getName());
		props.put("value.deserializer", StringDeserializer.class.getName());

		props.put("ssl.truststore.location", this.jksFilePath + "server-truststore-digicert.jks");// loadJksFile());
		props.put("ssl.truststore.password", this.trustStorePassword);

		props.put("sasl.mechanism", this.saslMechanism);
		props.put("sasl.kerberos.service.name", this.serviceName);
		props.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\""
				+ this.username + "\" password=\"" + this.password + "\";");

		System.out.println("Connecting to IntelSat Kafka Env: " + env);
		System.out.println("Kafka Bootstrap Server: " + this.bootstrapServer);
		System.out.println("Kafka Topic: " + this.topicName);
		System.out.println("Kafka Consumer Group Name: " + this.consumerGroup);
		System.out.println("Kafka Username: " + this.username);
		// System.out.print("Password: "+this.password);

		try {
			this.kafkaConsumer = new KafkaConsumer(props);
			System.out.println("4. Creating Kafka Consumer Client -> SUCCESS");
		} catch (Exception e) {
			System.out.println("4. Creating Kafka Consumer Client -> FAILURE");
			endProgram(e.getCause().getMessage());
		}

		try {
			this.kafkaConsumer.subscribe(Collections.singletonList(this.topicName));
			System.out.println("5. Subscribing to topic: " + this.topicName + " -> SUCCESS");
		} catch (Exception e) {
			System.out.println("5. Subscribing to topic: " + this.topicName + "FAILURE");
			endProgram(e.getCause().getMessage());
		}
	}

	private void endProgram(String failureReason) {
		if (!failureReason.isEmpty()) {
			System.out.println();
			System.out.println("*****" + failureReason);
			System.exit(-1);
		}
	}
}
