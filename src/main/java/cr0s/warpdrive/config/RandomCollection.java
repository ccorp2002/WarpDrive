package cr0s.warpdrive.config;

import cr0s.warpdrive.WarpDrive;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import net.minecraft.util.IStringSerializable;

/**
 * Collection of elements with ratios and weights. Helps to select element with controlled odds.
 * 
 * @author ncrashed, LemADEC
 *
 * @param <E>
 **/
public class RandomCollection<E extends IStringSerializable> {
	private final NavigableMap<Integer, E> weightMap = new TreeMap<>();
	private int totalWeight = 0;
	private final NavigableMap<Double, E> ratioMap = new TreeMap<>();
	private double totalRatio = 0;
	private final ArrayList<E> list = new ArrayList<>();
	
	/**
	 * Add new object and its weight.
	 * 
	 * @param weight
	 *            Used for random pick. The higher the value is relatively to others, the higher odds of choosing the object.
	 * @param object
	 *            Object to add
	 **/
	public void addWeight(final int weight, final E object) {
		if (weight <= 0) {
			WarpDrive.logger.warn("Weight is negative or zero, skipping " + object + " with weight " + weight);
			return;
		}
		if (weightMap.containsValue(object)) {
			if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
				WarpDrive.logger.trace("Object already has a weight defined, skipping " + object + " with weight " + weight);
			}
			return;
		}
		totalWeight += weight;
		weightMap.put(totalWeight, object);
		list.add(object);
	}
	
	/**
	 * Add new object and its ratio. Warning: if total ratio goes higher than 1.0, element won't be added to collection.
	 * 
	 * @param ratio
	 *            Chance of random pick in range (0, 1.0]. In contrast to weights, ratio is fixed and chances don't change if you add more elements.
	 * @param object
	 *            Object to add
	 **/
	public void addRatio(final double ratio, final E object) {
		if (ratio <= 0 || ratio >= 1.0) {
			WarpDrive.logger.warn("Ratio isn't in ]0, 1.0] bounds, skipping " + object + " with ratio " + ratio);
			return;
		}
		if (ratioMap.containsValue(object)) {
			if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
				WarpDrive.logger.warn("Object already has a ratio defined, skipping " + object + " with ratio " + ratio);
			}
			return;
		}
		
		if (totalRatio + ratio > 1.0) {
			WarpDrive.logger.warn("Total ratio is greater than 1.0, skipping " + object + " with ratio " + ratio);
			return;
		}
		totalRatio += ratio;
		ratioMap.put(totalRatio, object);
		list.add(object);
	}
	
	public E getRandomEntry(final Random random) {
		double value = random.nextDouble();
		
		if (totalWeight == 0.0D) {
			value *= totalRatio;
		}
		
		if (value < totalRatio) { // hit ratio part of values
			return ratioMap.ceilingEntry(value).getValue();
		} else { // hit dynamic part of values, weighted ones
			final int weight = (int)Math.round((value - totalRatio) * totalWeight);
			final Entry<Integer, E> entry = weightMap.ceilingEntry(weight);
			/*
			WarpDrive.logger.info("value " + String.format("%.3f", value)
					+ " => " + entry + " totals "
					+ totalRatio + " " + totalWeight
					+ " " + Arrays.toString(weightMap.navigableKeySet().toArray())
					+ " " + Arrays.toString(weightMap.values().toArray()));
			/**/
			if (entry != null) {
				return entry.getValue();
			} else {
				return null;
			}
		}
	}
	
	/**
	 * Get a specific object through its name
	 * 
	 * @param name Exact name of the object
	 * @return Named object or null if there is no object with that name
	 **/
	public E getNamedEntry(final String name) {
		for (final E object : list) {
			if (object.getName().equals(name)) {
				return object;
			}
		}
		return null;
	}
	
	/**
	 * Get a string listing all object names
	 * 
	 * @return Formatted string list separated by commas
	 **/
	public String getNames() {
		String names = "";
		if (list.isEmpty()) {
			return "-none defined-";
		}
		for (final E object : list) {
			if (!names.isEmpty()) {
				names += ", ";
			}
			names += object.getName();
		}
		return names;
	}
	
	/**
	 * @return All registered objects
	 **/
	public ArrayList<E> elements() {
		return list;
	}
	
	/**
	 * Add an object for weighted pick.
	 * 
	 * @param object
	 *            Object to load into
	 * @param stringRatio
	 *            Element of an XML file
	 * @throws InvalidParameterException
	 **/
	public void add(final E object, final String stringRatio, final String stringWeight) throws InvalidParameterException {
		// detect and handle loading of an existing object
		final E existing = getNamedEntry(object.getName());
		if (existing != null) {
			if (existing.equals(object)) {
				// all good, nothing to do
				if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.info("Object already exists in collection, skipping " + object.getName());
				}
				return;
			} else {
				throw new InvalidParameterException("Invalid merge of different objects with the same name " + object.getName()
						+ "\nnew entry is " + object
						+ "\nwhile existing entry is " + existing + "");
			}
		}
		
		// ratio takes priority over weight
		if (stringRatio != null && !stringRatio.isEmpty()) {
			final double ratio;
			try {
				ratio = Double.parseDouble(stringRatio);
			} catch (final NumberFormatException exceptionRatio) {
				throw new InvalidParameterException("Ratio must be double!");
			}
			addRatio(ratio, object);
			
		} else { // defaults to weight=1
			int weight = 1;
			if (stringWeight != null && !stringWeight.isEmpty()) {
				try {
					weight = Integer.parseInt(stringWeight);
				} catch (final NumberFormatException exceptionWeight) {
					throw new InvalidParameterException("Weight must be an integer!");
				}
				weight = Math.max(1, weight);
			}
			addWeight(weight, object);
		}
	}
	
	public void loadFrom(final RandomCollection<E> objects) {
		int previousWeight = 0;
		for (final Entry<Integer, E> entry : objects.weightMap.entrySet()) {
			addWeight(entry.getKey() - previousWeight, entry.getValue());
			previousWeight = entry.getKey();
		}
		double previousRatio = 0.0D;
		for (final Entry<Double, E> entry : objects.ratioMap.entrySet()) {
			addRatio(entry.getKey() - previousRatio, entry.getValue());
			previousRatio = entry.getKey();
		}
	}
	
	/**
	 * Return true when no content has been provided yet
	 * 
	 * @return isEmpty
	 **/
	public boolean isEmpty() {
		return list.isEmpty();
	}
}