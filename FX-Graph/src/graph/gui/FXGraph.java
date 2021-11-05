package graph.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import graph.annotations.NotNull;
import graph.annotations.Nullable;
import graph.dataclasses.GraphTriple;
import graph.dataclasses.GraphLayout;
import graph.dataclasses.WeightConverter;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

public class FXGraph<T, K> extends Group {

	private final ObservableSet<Node<T>> nodes = FXCollections.observableSet(new HashSet<>());
	private final ObservableMap<Node<T>, ObservableMap<Node<T>, Edge<K>>> edges = FXCollections.observableHashMap();

	private final boolean digraph;
	private boolean lock;

	// cache variables to store node focused and edge focused
	private final ObjectProperty<Node<T>> nodeFocused = new SimpleObjectProperty<>();
	private final ObjectProperty<GraphTriple<Node<T>, Edge<K>, Node<T>>> edgeFocused = new SimpleObjectProperty<>();

	/**
	 * Create a new graph.
	 * 
	 * @param nodes values of nodes. Note: The graphic value will be
	 * 
	 *              <pre>
	 *              Object.toString()
	 *              </pre>
	 * 
	 *              Note: more nodes can have same graphic value, but duplicates
	 *              objects are not allowed!
	 */

	public FXGraph(boolean digraph) {
		this(new HashMap<>(), digraph);
	}

	public FXGraph(@NotNull HashMap<T, Node<T>> nodes, boolean digraph) {
		this(nodes.values(), digraph);
	}

	public FXGraph(@NotNull Node<T>[] nodes, boolean digraph) {
		this(Arrays.asList(nodes), digraph);
	}

	public FXGraph(@NotNull Collection<Node<T>> nodes, boolean digraph) {
		checkThread();
		this.digraph = digraph;

		// listen for add or remove graphic node
		this.nodes.addListener(this::listenNodeChange);
		addEventFilter(KeyEvent.KEY_PRESSED, this::translationAnimation);

		// create nodes
		if (nodes != null)
			for (Node<T> n : nodes)
				addNode(n);
	}

	public void setGraphLayout(@NotNull Rectangle2D range, @NotNull GraphLayout gl) {
		Objects.requireNonNull(range);
		Objects.requireNonNull(gl);

		if (gl == GraphLayout.RANDOM)
			nodes.forEach(e -> Node.shuffle(range, e));
	}

	/**
	 * get an unmodifiable set of all nodes registered in the graph
	 * 
	 * @return an unmodifiable set of nodes
	 */

	@NotNull
	public final Set<Node<T>> getNodes() {
		return Collections.unmodifiableSet(nodes);
	}

	/**
	 * get the edge which connects 2 nodes
	 * 
	 * @param n1 node 1
	 * @param n2 node 2
	 * @return the edge which connects the 2 nodes
	 */

	@Nullable
	public final Edge<K> getEdge(@NotNull Node<T> n1, @NotNull Node<T> n2) {
		return edges.get(n1).get(n2);
	}

	private void relax(Node<T> n1, Node<T> n2, K weight, WeightConverter<K> converter) {
		double converted = converter.convert(weight); // convert weight into a number
		if (n2.time > n1.time + converted) {
			// reset color if new edge with lower weight is found
			if (n2.parent != null)
				edges.get(n2.parent).get(n2).setStroke(Color.BLACK);

			// change the color of the edge with lower weight to red
			edges.get(n1).get(n2).setStroke(Color.RED);
			n2.time = n1.time + converted;
			n2.parent = n1;
		}
	}

	/**
	 * apply bellman ford algorithm to find min tree walk
	 * 
	 * @param root      a node to start bellmanford algorithm
	 * @param converter a converter used to convert generic object weight to a
	 *                  number weight.
	 * @return True if no cycle of negative weight is found. False otherwise
	 */

	public final boolean bellmanFord(@NotNull Node<T> root, @NotNull WeightConverter<K> converter) {
		checkThread();

		Objects.requireNonNull(root);
		Objects.requireNonNull(converter);

		// reset all nodes
		for (Node<T> n : nodes) {

			if (edges.get(n) != null)
				edges.get(n).values().forEach(e -> e.setStroke(Color.BLACK));

			n.time = Double.POSITIVE_INFINITY;
			n.parent = null;
		}

		root.parent = null;
		root.time = 0;

		// execute bellmanford algorithm
		for (int i = 0; i < nodes.size(); i++)
			for (Node<T> n1 : edges.keySet())
				for (Node<T> n2 : edges.get(n1).keySet())
					relax(n1, n2, edges.get(n1).get(n2).getWeight(), converter);

		for (Node<T> n1 : edges.keySet())
			for (Node<T> n2 : edges.get(n1).keySet())
				if (n1.time > n2.time + converter.convert(edges.get(n1).get(n2).getWeight()))
					return false;

		// fix graphic issues
		for (Node<T> n : nodes) {
			n.setLayoutX(n.getLayoutX() - 1);
			n.setLayoutX(n.getLayoutX() + 1);
			n.setLayoutY(n.getLayoutY() - 1);
			n.setLayoutY(n.getLayoutY() + 1);
		}
		return true;

	}

	/**
	 * 
	 * @param value Duplicate nodes are allowed. Ever if instance is different
	 */

	@NotNull
	public final FXGraph<T, K> addNode(@NotNull Node<T> value) {
		checkThread();

		Objects.requireNonNull(value);

		if (nodes.contains(value))
			throw new IllegalArgumentException("Node duplicate");

		// save the current focused node
		value.focusedProperty().addListener((o, old, neww) -> nodeFocused.set(value));

		// listen for new edges to draw them
		value.out.addListener(this::listenEdgeChange);
		nodes.add(value);

		return this;
	}

	/**
	 * @param short way to create multiple nodes in one call see
	 *              {@link #addNode(Object)}
	 */

	@SafeVarargs
	@NotNull
	public final FXGraph<T, K> addNodes(@NotNull Node<T>... values) {
		checkThread();

		Objects.requireNonNull(values);

		for (int i = 0; i < values.length; i++)
			addNode(values[i]);

		return this;
	}

	/**
	 * 
	 * @param n1 node 1
	 * @param n2 node 2
	 * @param w  edge weight
	 * @throws IllegalArgumentException if node doesn't exists
	 * @return
	 */

	@NotNull
	public final FXGraph<T, K> newEdge(@NotNull Node<T> n1, @NotNull Node<T> n2, @NotNull K w) {
		checkThread();

		Objects.requireNonNull(n1);
		Objects.requireNonNull(n2);

		// create edge n1 -> n2
		createEdge(n1, n2, w);

		// create edge n1 <- n2 if it's not a digraph
		if (!digraph)
			createEdge(n2, n1, w);

		n1.toFront();
		n2.toFront();

		return this;
	}

	private void createEdge(Node<T> n1, Node<T> n2, K weight) {
		// check if edge already exists
		if (edges.get(n1) != null && edges.get(n1).get(n2) != null) {
			edges.get(n1).get(n2).setWeight(weight);
			return;
		}

		Edge<K> arrow = new Edge<K>(n1, n2, n2.getPrefWidth() / 2, weight);
		n2.in.put(n1, arrow);
		n1.out.put(n2, arrow);

		arrow.setStrokeWidth(4);

		arrow.setOnMouseClicked(e -> edgeFocused.set(new GraphTriple<>(n1, arrow, n2)));
		// store the edge
		if (edges.get(n1) == null)
			edges.put(n1, FXCollections.observableHashMap());

		edges.get(n1).put(n2, arrow);
	}

	/**
	 * remove the edge wich connect the 2 non null nodes
	 * 
	 * @throws IllegalArgumentException if one node is null or if edge doesn't
	 *                                  exists
	 * @param n1
	 * @param n2
	 * @return
	 */

	@NotNull
	public FXGraph<T, K> removeEdge(@NotNull Node<T> n1, @NotNull Node<T> n2) {
		checkThread();

		Objects.requireNonNull(n1);
		Objects.requireNonNull(n2);

		if (edges.get(n1) == null)
			throw new IllegalArgumentException("Node null");

		edges.get(n1).remove(n2);
		n1.out.remove(n2);
		n2.in.remove(n1);

		return this;
	}

	/**
	 * delete a node from the graph
	 * 
	 * @param n
	 * @return
	 */

	@NotNull
	public FXGraph<T, K> removeNode(@NotNull Node<T> n) {
		checkThread();

		if (n == null)
			throw new IllegalArgumentException("Node null");

		nodes.remove(n);
		if (edges.get(n) != null)
			edges.get(n).clear();

		n.out.clear();

		for (Node<T> in : n.in.keySet())
			in.out.remove(n);

		n.in.clear();
		return this;
	}

	@SafeVarargs
	@NotNull
	public final FXGraph<T, K> removeNodes(@NotNull Node<T>... nodes) {
		checkThread();

		Objects.requireNonNull(nodes);

		for (int i = 0; i < nodes.length; i++)
			removeNode(nodes[i]);

		return this;
	}

	/**
	 * lock graph translation event
	 */

	public void lock() {
		lock = true;
	}

	/**
	 * unlock graph transtation event
	 */

	public void unlock() {
		lock = false;
	}

	/**
	 * @param url URL to load
	 * @throws IllegalStateException if no scene is associated to this component.
	 *                               This method should be called only after scene
	 *                               creation
	 */

	private void translationAnimation(KeyEvent e) {
		if (lock)
			return;

		for (Node<T> n : nodes)
			switch (e.getCode()) {
			case UP:
			case W:
				n.setLayoutY(n.getLayoutY() + 8);
				break;
			case RIGHT:
			case D:
				n.setLayoutX(n.getLayoutX() - 8);
				break;
			case DOWN:
			case S:
				n.setLayoutY(n.getLayoutY() - 8);
				break;
			case LEFT:
			case A:
				n.setLayoutX(n.getLayoutX() + 8);
				break;
			default:
				break;
			}

		e.consume();
	}

	private final void listenNodeChange(SetChangeListener.Change<? extends Node<?>> c) {
		if (c.wasAdded())
			getChildren().add(c.getElementAdded());

		if (c.wasRemoved())
			getChildren().remove(c.getElementRemoved());
	}

	private final void listenEdgeChange(MapChangeListener.Change<? extends Node<?>, ? extends Edge<?>> c) {
		if (c.wasAdded())
			getChildren().add(c.getValueAdded());

		if (c.wasRemoved())
			getChildren().remove(c.getValueRemoved());
	}

	private static void checkThread() {
		if (!Platform.isFxApplicationThread())
			throw new IllegalStateException("Not on JavaFX application thread");
	}
}
