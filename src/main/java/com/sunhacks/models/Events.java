package com.sunhacks.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "events")
public class Events {
	@Id
	private String name;
	private String description, latitude, longitude, place;
	private int travelling_time=0, rating;
	private String event_strt_time;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	public String getPlace() {
		return place;
	}
	public void setPlace(String place) {
		this.place = place;
	}
	public String getEvent_strt_time() {
		return event_strt_time;
	}
	public void setEvent_strt_time(String event_strt_time) {
		this.event_strt_time = event_strt_time;
	}
	public int getTravelling_time() {
		return travelling_time;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	public void setTravelling_time(int travelling_time) {
		this.travelling_time = travelling_time;
	}

	@Override
	public String toString() {
		return "Events{" +
				"name='" + name + '\'' +
				", description='" + description + '\'' +
				", latitude='" + latitude + '\'' +
				", longitude='" + longitude + '\'' +
				", place='" + place + '\'' +
				", event_strt_time=" + event_strt_time +
				", travelling_time=" + travelling_time +
				", rating=" + rating
				+"'";
	}
}