package com.sunhacks.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunhacks.models.Event;
import com.sunhacks.repository.EventRepository;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class HomeController {
	@Autowired
	private EventRepository repository;

	@RequestMapping(value = "/saveEvent", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
	public String index() {
		Event e = new Event();
		e.setEventName("Sunhacks");
		e.setEventLink("Hacktathon");
//        e.setEvent_strt_time(1232312);
		repository.save(e);
		StringBuilder sb = new StringBuilder("");
		for (Event Event : repository.findAll()) {
			sb.append(Event.toString());
		}
		return sb.toString();
	}

	@RequestMapping(value = "/historyEvents", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
	public String getHistoryEvents() {
		ObjectMapper mapper = new ObjectMapper();
		List<Event> list = repository.findAll();
		String jsonInString = "";
		try {
			jsonInString = mapper.writeValueAsString(list);
		} catch (JsonProcessingException j) {
			jsonInString = "";
		}
		return jsonInString;
	}

	@RequestMapping(value = "/saveRatings")
	public boolean saveRatings() {
		ObjectMapper mapper = new ObjectMapper();
		String id = "Sunhacks";
		Event event = repository.findOne(id);
		event.setEventRating(5);
		try {
			repository.save(event);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@RequestMapping(value = "/getEvents", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
	public String getEvents(@RequestBody String request) throws JsonProcessingException, IOException, ParseException {
		
		System.out.println("POST Request" + request);
		RestTemplate restTemplate = new RestTemplate();
		ObjectMapper mapper = new ObjectMapper();
		
//		boolean rightNow = true; // default should be false
		String requestLocation = "Tempe"; // default value is "default"
		String requestLatitude = "33.4255";
		String requestLongitude = "-111.9400";
		String searchRadius = "10";
		long requestDateTime = -1;
		int noOfDays = -1; // default value is -1"43.874668","-81.484383"
		int noOfHours = 5;
		
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String startDate = null;
		String endDate = null;
		
		long systemTime = System.currentTimeMillis();

		if((!(requestLocation.equals("default"))) && noOfDays != -1 && requestDateTime != -1) {
			Timestamp timestamp = new Timestamp(systemTime);
			startDate = simpleDateFormat.format(timestamp);
			timestamp = new Timestamp(systemTime + noOfDays * 24 * 3600 * 1000 - noOfHours * 3600 * 1000);
			endDate = simpleDateFormat.format(timestamp);
		}else {
			Timestamp timestamp = new Timestamp(systemTime);
			startDate = simpleDateFormat.format(timestamp);
			timestamp = new Timestamp(systemTime + noOfHours * 3600 * 1000);
			endDate = simpleDateFormat.format(timestamp);
		}
		
		String discoveryApi = "https://app.ticketmaster.com/discovery/v2/events.json?latlong=" + requestLatitude + "," + requestLongitude + "&radius=" + searchRadius + "&startDateTime=" + startDate + "&endDateTime" + endDate + "&apikey=MUoKA8DyO4d1TsiK8TDreOQG1tIOHbHD";
		System.out.println(discoveryApi);
		ResponseEntity<String> response = restTemplate.getForEntity(discoveryApi, String.class);
		JsonNode root = mapper.readTree(response.getBody());
		JsonNode name = root.path("_embedded").path("events");

		List<Event> eventList = new ArrayList<>();

		if (name.isArray()) {
			for (final JsonNode objNode : name) {

				if (objNode.path("dates").path("start").get("dateTime") == null) {
					System.out.println("dateTime null");
					continue;
				}

				Event event = new Event();

				event.setEventName(objNode.get("name").asText());
				event.setEventPlace(objNode.path("_embedded").path("venues").get(0).get("name").asText());
				event.setEventLatitude(
						objNode.path("_embedded").path("venues").get(0).path("location").get("latitude").asText());
				event.setEventLongitude(
						objNode.path("_embedded").path("venues").get(0).path("location").get("longitude").asText());
				event.setEventLink(objNode.get("url").asText());

				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				Date dt = sdf.parse(objNode.path("dates").path("start").get("dateTime").asText());
				long epoch = dt.getTime();
				event.setEventStartTime(epoch / 1000);
				event.setEventDuration((int) ((Math.random() * 240) + 180) * 60);

				eventList.add(event);
			}
		}
		
		
		List<Event> feasibleEvents;

		if ((!(requestLocation.equals("default"))) && noOfDays != -1 && requestDateTime != -1) {
			System.out.println("planned");
			feasibleEvents = generateFeasibleEvents(eventList, requestLocation, noOfHours, noOfDays);
		} else {
			System.out.println("right now");
			feasibleEvents = generateFeasibleEvents(eventList, "43.874668", "-81.484383", noOfHours);
		}

		String jsonInString = "";
		try {
			jsonInString = mapper.writeValueAsString(feasibleEvents);
		} catch (JsonProcessingException j) {
			jsonInString = "";
		}
		return jsonInString;
	}

	private List<Event> generateFeasibleEvents(List<Event> eventList, String requestLocation, int noOfHours,
			int noOfDays) throws JsonProcessingException, IOException {
		
		String requests = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + requestLocation;
		
		RestTemplate restTemplate = new RestTemplate();
		ObjectMapper mapper = new ObjectMapper();

		requests += "&destinations=";

		for (int i = 0; i < eventList.size(); i++) {
			requests += Float.parseFloat(eventList.get(i).getEventLatitude()) + ","
					+ Float.parseFloat(eventList.get(i).getEventLongitude()) + "|";
		}
		requests = requests.substring(0, requests.length() - 1);
		requests += "&key=AIzaSyAq9QsLNB4AcqvPmLgVhR22CIAznd2Y3uM";

		System.out.println(requests);

		ResponseEntity<String> response = restTemplate.getForEntity(requests, String.class);

		JsonNode root = mapper.readTree(response.getBody());
		JsonNode destinations = root.path("rows").get(0).path("elements");

		List<Event> feasibleEvents = new ArrayList<Event>();

		int i = 0;
		for (final JsonNode objNode : destinations) {

			long travelTime = Long.parseLong(objNode.path("duration").get("value").asText());
			long timestamp = System.currentTimeMillis() / 1000;

			System.out.println((travelTime + timestamp) + " " + eventList.get(i).getEventStartTime());

			if ((timestamp + travelTime < eventList.get(i).getEventStartTime()) 
					&& (eventList.get(i).getEventDuration() + 2 * eventList.get(i).getTravellingTime() < (noOfHours * 3600)) 
					&& (eventList.get(i).getEventStartTime() + noOfHours * 3600 < timestamp + noOfDays * 24 * 3600)) {
				eventList.get(i).setTravellingTime(travelTime);
				feasibleEvents.add(eventList.get(i));
			}

			i++;
		}

//		return feasibleEvents.subList(0, Math.min(feasibleEvents.size(), 5));
		return feasibleEvents;
	}

	public List<Event> generateFeasibleEvents(List<Event> eventList, String requestLatitude, String requestLongitude,
			int noOfHours) throws JsonProcessingException, IOException {

		String requests = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + requestLatitude + ","
				+ requestLongitude;
		RestTemplate restTemplate = new RestTemplate();
		ObjectMapper mapper = new ObjectMapper();

		requests += "&destinations=";

		for (int i = 0; i < eventList.size(); i++) {
			requests += Float.parseFloat(eventList.get(i).getEventLatitude()) + ","
					+ Float.parseFloat(eventList.get(i).getEventLongitude()) + "|";
		}
		requests = requests.substring(0, requests.length() - 1);
		requests += "&key=AIzaSyAq9QsLNB4AcqvPmLgVhR22CIAznd2Y3uM";

		System.out.println(requests);

		ResponseEntity<String> response = restTemplate.getForEntity(requests, String.class);

		JsonNode root = mapper.readTree(response.getBody());
		JsonNode destinations = root.path("rows").get(0).path("elements");

		List<Event> feasibleEvents = new ArrayList<Event>();

		int i = 0;
		for (final JsonNode objNode : destinations) {

			long travelTime = Long.parseLong(objNode.path("duration").get("value").asText());
			long timestamp = System.currentTimeMillis() / 1000;

			System.out.println((travelTime + timestamp) + " " + eventList.get(i).getEventStartTime());
			
//			eventList.get(i).setTravellingTime(travelTime);
//			feasibleEvents.add(eventList.get(i));

			if ((timestamp + travelTime < eventList.get(i).getEventStartTime()) && (eventList.get(i).getEventDuration()
					+ 2 * eventList.get(i).getTravellingTime()) < (noOfHours * 3600)) {
				eventList.get(i).setTravellingTime(travelTime);
				feasibleEvents.add(eventList.get(i));
			}

			i++;
		}

//		return feasibleEvents.subList(0, Math.min(feasibleEvents.size(), 5));
		return feasibleEvents;
	}
}
