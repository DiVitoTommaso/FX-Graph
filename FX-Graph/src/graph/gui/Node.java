package graph.gui;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

import graph.annotations.NotNull;
import graph.annotations.Nullable;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Node<T> extends Button {

	final ObservableMap<Node<T>, Edge<?>> in = FXCollections.observableHashMap();
	final ObservableMap<Node<T>, Edge<?>> out = FXCollections.observableHashMap();

	Node<T> parent;
	double time;

	private final T val;
	private final int hashcode;
	private boolean lock;

	/**
	 * Hashable object.This object can be used as key in dictionaries, even if value
	 * is not immutable because the hashcode will be generated only on creation.
	 * 
	 * @param v value of the node
	 */
	public Node(@NotNull T v) {
		if (!Platform.isFxApplicationThread())
			throw new IllegalStateException("Not on JavaFX application thread");

		Objects.requireNonNull(v);

		val = v;
		setPrefSize(120, 120);

		// set graphic value
		setText(v.toString());

		// change border color when focused
		focusedProperty().addListener((o, old, neww) -> {
			if (neww.booleanValue())
				setStyle(
						"-fx-background-radius: 60; -fx-background-color: white; -fx-border-color: blue; -fx-border-radius: 60; -fx-border-width: 4");
			else
				setStyle(
						"-fx-background-radius: 60; -fx-background-color: white; -fx-border-color: black; -fx-border-radius: 60; -fx-border-width: 4");

		});

		layoutXProperty().addListener(this::updateEdges);
		layoutYProperty().addListener(this::updateEdges);

		// move the node when dragged and all edges attached
		setOnMouseDragged(this::dragAnimation);

		// turn button into a circle
		setFont(Font.font("verbena", FontWeight.BOLD, 24));
		setStyle(
				"-fx-background-radius: 50; -fx-background-color: white; -fx-border-color: black; -fx-border-radius: 50; -fx-border-width: 3");

		hashcode = val.hashCode();
	}

	/**
	 * enable node drag and drop
	 */

	public void lock() {
		lock = true;
	}

	/**
	 * disable node drag and drop
	 */

	public void unlock() {
		lock = false;
	}

	/**
	 * get the value of this instance
	 * 
	 * @return the value of the node
	 */

	public final T getValue() {
		return val;
	}

	/**
	 * Even if the object is mutable, this method return the {@link #hashcode} of
	 * the first evaluation when object is created. It means this object can be used
	 * has map key because it's hash won't never change
	 */

	public final int hashCode() {
		return hashcode;
	}

	/**
	 * two nodes are equals if the values of the nodes are equal
	 */

	public final boolean equals(@Nullable Object o2) {
		return o2 != null && o2.getClass() == Node.class && val.equals(((Node<?>) o2).val);
	}

	/**
	 * create an array of nodes
	 * 
	 * @param values node values
	 * @return Node array created using values keeping default order
	 */

	@SafeVarargs
	@SuppressWarnings("unchecked")
	@NotNull
	public static <T> Node<T>[] array(@NotNull T... values) {
		Objects.requireNonNull(values);

		Node<T>[] nodes = (Node<T>[]) Array.newInstance(Node.class, values.length);
		for (int i = 0; i < values.length; i++)
			nodes[i] = new Node<T>(values[i]);
		return nodes;
	}

	/**
	 * create a map of nodes
	 * 
	 * @param values node values
	 * @return map of node with keys the values and value the node object
	 */

	@SafeVarargs
	@NotNull
	public static <T> HashMap<T, Node<T>> map(@NotNull T... values) {
		Objects.requireNonNull(values);

		Node<T>[] tmp = array(values);
		return asMap(tmp);

	}

	/**
	 * turn a node array into a map
	 * 
	 * @param nodes the node array to turn into a map
	 * @return see {@link #map(Object...)}
	 */

	@SafeVarargs
	@NotNull
	public static <T> HashMap<T, Node<T>> asMap(@NotNull Node<T>... nodes) {
		Objects.requireNonNull(nodes);

		HashMap<T, Node<T>> map = new HashMap<>();
		for (Node<T> n : nodes)
			map.put(n.val, n);

		return map;
	}

	/**
	 * turn a node map into a array
	 * 
	 * @param nodes the node map to turn into an array
	 * @return see {@link #array(Object...)}
	 */

	@SuppressWarnings("unchecked")
	@NotNull
	public static <T> Node<T>[] asArray(@NotNull HashMap<T, Node<T>> nodes) {
		Objects.requireNonNull(nodes);

		return nodes.values().toArray((Node<T>[]) Array.newInstance(Node.class, 0));
	}

	private static Random r = new Random();

	/**
	 * randomize the position of the nodes inside the rectangle area
	 * 
	 * @param range area where nodes must be shuffled
	 * @param nodes the nodes to shuffle
	 */
	@SuppressWarnings("unchecked")
	public static <T> void shuffle(@NotNull Rectangle2D range, @NotNull HashMap<T, Node<T>> nodes) {
		Objects.requireNonNull(nodes);

		shuffle(range, nodes.values().toArray((Node<T>[]) Array.newInstance(Node.class, 0)));
	}

	/**
	 * randomize the position of the nodes inside the rectangle area
	 * 
	 * @param range area where nodes must be shuffled
	 * @param nodes the nodes to shuffle
	 */
	@SafeVarargs
	public static <T> void shuffle(@NotNull Rectangle2D range, @NotNull Node<T>... nodes) {

		for (Node<T> n : nodes) {
			n.setLayoutX(range.getMinX() + r.nextInt((int) (range.getMaxX() - range.getMinX())));
			n.setLayoutY(range.getMinY() + r.nextInt((int) (range.getMaxY() - range.getMinY())));
		}

	}

	private void updateEdges(ObservableValue<? extends Number> o, Number old, Number neww) {
		for (Edge<?> arrow : out.values()) {
			arrow.setStartX(getLayoutX() + getPrefWidth() / 2);
			arrow.setStartY(getLayoutY() + getPrefHeight() / 2);
		}

		for (Edge<?> arrow : in.values()) {
			arrow.setEndX(getLayoutX() + getPrefWidth() / 2);
			arrow.setEndY(getLayoutY() + getPrefHeight() / 2);
		}
	}

	private void dragAnimation(MouseEvent e) {
		if (lock)
			return;

		setLayoutX(e.getSceneX() - getPrefWidth() / 2);
		setLayoutY(e.getSceneY() - getPrefHeight() / 2);
		for (Edge<?> arrow : out.values()) {
			arrow.setStartX(getLayoutX() + getPrefWidth() / 2);
			arrow.setStartY(getLayoutY() + getPrefHeight() / 2);
		}

		for (Edge<?> arrow : in.values()) {
			arrow.setEndX(getLayoutX() + getPrefWidth() / 2);
			arrow.setEndY(getLayoutY() + getPrefHeight() / 2);
		}
	}

	@Override
	public String toString() {
		return val.toString();
	}
}
