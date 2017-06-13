package com.datastax.events.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;

import com.datastax.demo.utils.PropertyHelper;
import com.datastax.events.dao.EventDao;
import com.datastax.events.model.Event;

public class EventService {

	private EventDao dao;
	private ExecutorService executor = Executors.newFixedThreadPool(4);
	private AtomicLong counter = new AtomicLong(0);

	public EventService() {
		String contactPointsStr = PropertyHelper.getProperty("contactPoints", "localhost");
		this.dao = new EventDao(contactPointsStr.split(","));
	}

	public void getEvents(BlockingQueue<Event> queue, DateTime from, DateTime to, String eventType) {

		// Get all minutes between from and to dates
		DateTime time = from;

		while (time.isBefore(to)) {
			dao.getEventsForDate(queue, time, eventType);
			time = time.plusMinutes(1);
		}
	}

	public List<Event> getEvents(DateTime from, DateTime to) {
		return this.getEvents(from, to, null);
	}

	public List<Event> getEvents(DateTime from, DateTime to, String eventType) {

		final List<Event> events = new ArrayList<Event>();
		final BlockingQueue<Event> queue = new ArrayBlockingQueue<Event>(10000);

		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				while (true) {
					Event event = queue.poll();
					if (event != null) {
						events.add(event);
					}
				}
			}
		};

		executor.execute(runnable);

		// Get all minutes between from and to dates
		DateTime time = from;
		while (time.isBefore(to)) {
			dao.getEventsForDate(queue, time, eventType);

			time = time.plusMinutes(1);
		}

		return events;
	}

	public void insertEvent(Event event) {
		
		dao.insertEvent(event);
	}

	@Override
	public void finalize() {
		dao.close();
	}
}
