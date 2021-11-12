package graph.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import graph.annotations.NotNull;
import graph.annotations.Nullable;
import graph.dataclasses.GraphTriple;
import graph.dataclasses.FlowWeight;
import graph.dataclasses.GraphLayout;
import graph.dataclasses.WeightConverter;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

public class FXGraph<T, K> extends Group {

	private final ObservableList<Node<T>> nodes = FXCollections.observableList(new ArrayList<>());
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
	public final List<Node<T>> getNodes() {
		return Collections.unmodifiableList(nodes);
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

	public final double kruskal(@NotNull WeightConverter<K> conv) {
		checkThread();
		resetNodes();

		Objects.requireNonNull(conv);

		if (digraph)
			throw new IllegalStateException("Kruskal algorithm can be applied only to undirected graphs");

		ArrayList<Edge<K>> arr = new ArrayList<>();
		for (Map<Node<T>, Edge<K>> m : edges.values())
			arr.addAll(m.values());

		arr.sort((e1, e2) -> (int) (conv.convert(e1.getWeight()) - conv.convert(e2.getWeight())));

		for (int i = 0; i < nodes.size(); i++)
			nodes.get(i).time = i;

		ArrayList<Edge<K>> neww = new ArrayList<>();

		for (Edge<K> e : arr) {
			if (neww.size() == nodes.size() - 1)
				break;

			if (e.getNodeFrom().time != e.getNodeTo().time) {
				neww.add(e);
				double max = Math.max(e.getNodeFrom().time, e.getNodeTo().time);
				double min = Math.min(e.getNodeFrom().time, e.getNodeTo().time);
				for (Node<T> n : nodes)
					if (n.time == max)
						n.time = min;

			}

		}

		neww.forEach(e -> e.setStroke(Color.RED));

		double temp = 0;
		for (Edge<K> e : neww)
			temp += conv.convert(e.getWeight());

		return temp;

	}

	public final void dijkstra(@NotNull Node<T> root, @NotNull WeightConverter<K> conv) {
		checkThread();
		resetNodes();

		Objects.requireNonNull(root);
		Objects.requireNonNull(conv);

		root.time = 0;
		LinkedList<Node<T>> queue = new LinkedList<>(nodes);

		while (!queue.isEmpty()) {
			queue.sort((e1, e2) -> (int) (e1.time - e2.time));
			Node<T> n = queue.pop();

			for (Node<T> n2 : edges.get(n).keySet())
				relax(n, n2, edges.get(n).get(n2).getWeight(), conv);
		}

	}

	public final int fordFulkerson(@NotNull Node<T> root, @NotNull Node<T> end) {
		return fordFulkerson(root, end, false);
	}

	public final int fordFulkersonLogged(@NotNull Node<T> root, @NotNull Node<T> end) {
		return fordFulkerson(root, end, true);
	}

	public void bfs(@NotNull Node<T> root) {
		checkThread();
		resetNodes();
		Objects.requireNonNull(root);

		LinkedList<Node<T>> stack = new LinkedList<>(Arrays.asList(root));

		while (!stack.isEmpty()) {
			Node<T> pop = stack.removeLast();
			pop.time = 0;

			for (Node<T> n : edges.get(pop).keySet())
				if (n.time == Integer.MAX_VALUE) {
					n.parent = pop;
					stack.addLast(n);
				}
		}
	}

	public final int minFlow(@NotNull Map<Node<T>, Integer> ex, @NotNull Map<Node<T>, Integer> dx) {
		return minFlow(ex, dx, false);
	}

	public final int minFlowLogged(@NotNull Map<Node<T>, Integer> ex, @NotNull Map<Node<T>, Integer> dx) {
		return minFlow(ex, dx, true);
	}

	@SuppressWarnings("unchecked")
	private int minFlow(Map<Node<T>, Integer> ex, Map<Node<T>, Integer> dx, boolean doPrint) {
		checkThread();
		resetNodes();

		int sum1 = ex.values().stream().reduce(0, (e1, e2) -> e1 + e2);
		int sum2 = dx.values().stream().reduce(0, (e1, e2) -> e1 + e2);
		if (sum1 != -sum2)
			throw new IllegalStateException("Graph Ex nodes sum balances are not equal to Dx nodes sum balances");

		Objects.requireNonNull(ex);
		Objects.requireNonNull(dx);

		ArrayList<Edge<K>> arr = new ArrayList<>();

		for (Map<Node<T>, Edge<K>> m : edges.values())
			arr.addAll(m.values());

		arr.forEach(e -> {
			if (!(e.getWeight() instanceof FlowWeight))
				throw new IllegalStateException("Graph edge weights are not instance of FlowWeight");
		});

		ArrayList<Node<T>> exNodes = new ArrayList<>(ex.keySet());

		Node<T> source = new Node<T>((T) "[SOURCE]");

		addNode(source);
		for (Node<T> n : ex.keySet())
			newEdge(source, n, (K) new FlowWeight(0, Integer.MAX_VALUE));

		int minCost = 0;
		while (!ex.isEmpty()) {

			bellmanFord(source, e -> ((FlowWeight) e).flow);

			Node<T> end = null;

			for (Node<T> n : dx.keySet())
				if (end == null)
					end = n;
				else
					end = n.time < end.time ? n : end;

			if (end.time == Integer.MAX_VALUE)
				throw new IllegalStateException("Given graph has no eligible flows");

			ArrayList<Node<T>> walk = getWalk(end);

			int flow = getMinMaxFlow(walk);

			flow = Math.min(flow, ex.get(walk.get(1)));
			flow = Math.min(flow, -dx.get(end));

			minCost += updateBalances(walk, flow, ex, dx);

			applyMinFlow(walk, flow, doPrint);

			if (!ex.containsKey(walk.get(1)))
				removeEdge(source, walk.get(1));

			String v = "[";

			for (Node<T> n : nodes)
				if (ex.containsKey(n))
					v += ex.get(n) + ",";
				else if (dx.containsKey(n))
					v += dx.get(n) + ",";
				else
					v += "0,";

			if (doPrint)
				System.out.println(v.substring(0, v.length() - 1) + "]");
		}

		if (doPrint)
			System.out.println("END ALGORITHM");

		removeNode(source);
		for (Node<T> n : exNodes)
			removeEdge(source, n);

		resetNodes();

		return minCost;

	}

	private int getMinMaxFlow(ArrayList<Node<T>> walk) {
		int min = Integer.MAX_VALUE;

		if (walk.size() == 1)
			return min;

		for (int i = 0; i < walk.size() - 1; i++) {
			int available = ((FlowWeight) edges.get(walk.get(i)).get(walk.get(i + 1)).getWeight()).capacity;
			min = (int) Math.min(min, available);
		}

		return min;

	}

	@SuppressWarnings("unchecked")
	private void applyMinFlow(ArrayList<Node<T>> walk, int flow, boolean doPrint) {
		for (int i = 0; i < walk.size() - 1; i++) {
			Node<T> from = walk.get(i);
			Node<T> to = walk.get(i + 1);

			Edge<K> edge = edges.get(from).get(to);
			((FlowWeight) edge.getWeight()).capacity -= flow;

			if (((FlowWeight) edge.getWeight()).capacity == 0) {

				if (doPrint)
					System.out.print(edges.get(from).get(to) + " => " + flow + " {FULL} => ");

				removeEdge(from, to);
			} else {

				if (doPrint)
					System.out.print(edges.get(from).get(to) + " => +" + flow + " => ");
			}

			FlowWeight f = (FlowWeight) edge.getWeight();
			if (edges.get(to).get(from) == null)
				newEdge(to, from, (K) new FlowWeight(-f.flow, f.flow));
			else
				((FlowWeight) edges.get(to).get(from).getWeight()).capacity += flow;
		}

		if (doPrint)
			System.out.print("END ITERATION => ");

	}

	private int updateBalances(ArrayList<Node<T>> walk, int flow, Map<Node<T>, Integer> ex, Map<Node<T>, Integer> dx) {
		int cost = 0;
		ex.put(walk.get(1), ex.get(walk.get(1)) - flow);
		if (ex.get(walk.get(1)) == 0) {
			ex.remove(walk.get(1));
		}

		dx.put(walk.get(walk.size() - 1), dx.get(walk.get(walk.size() - 1)) + flow);
		if (dx.get(walk.get(walk.size() - 1)) == 0)
			dx.remove(walk.get(walk.size() - 1));

		for (int i = 0; i < walk.size() - 1; i++)
			cost += ((FlowWeight) edges.get(walk.get(i)).get(walk.get(i + 1)).getWeight()).flow;

		return cost;

	}

	public int edmondsKarp(@NotNull Node<T> root, @NotNull Node<T> end) {
		return edmondsKarp(root, end, false);
	}

	public int edmondsKarpLogged(@NotNull Node<T> root, @NotNull Node<T> end) {
		return edmondsKarp(root, end, true);
	}

	private int edmondsKarp(@NotNull Node<T> root, @NotNull Node<T> end, boolean doPrinter) {
		checkThread();
		resetNodes();

		Objects.requireNonNull(root);
		Objects.requireNonNull(end);

		int flow = 0;
		ArrayList<Edge<K>> arr = new ArrayList<>();

		for (Map<Node<T>, Edge<K>> m : edges.values())
			arr.addAll(m.values());

		arr.forEach(e -> {
			if (!(e.getWeight() instanceof FlowWeight))
				throw new IllegalStateException("Graph edge weights are not instance of FlowWeight");
		});

		arr.clear();

		do {
			bfs(root);
			ArrayList<Node<T>> walk = getWalk(end);
			int min = getMaxFlow(walk);

			if (min != Integer.MAX_VALUE) {
				applyFlow(walk, min, doPrinter);
				flow += min;
			}
		} while (end.time == 0);

		if (doPrinter)
			System.out.println("END ALGORITHM");

		for (Map<Node<T>, Edge<K>> m : edges.values())
			arr.addAll(m.values());

		minCut(root);

		arr.forEach(e -> {
			FlowWeight w = (FlowWeight) e.getWeight();
			if (w.getAvailable() == 0)
				e.setStroke(Color.RED);
			else if (w.flow != 0)
				e.setStroke(Color.BLUE);
			else
				e.setStroke(Color.BLACK);

		});

		return flow;

	}

	private int fordFulkerson(@NotNull Node<T> root, @NotNull Node<T> end, boolean doPrinter) {
		checkThread();
		resetNodes();

		Objects.requireNonNull(root);
		Objects.requireNonNull(end);

		int flow = 0;
		ArrayList<Edge<K>> arr = new ArrayList<>();

		for (Map<Node<T>, Edge<K>> m : edges.values())
			arr.addAll(m.values());

		arr.forEach(e -> {
			if (!(e.getWeight() instanceof FlowWeight))
				throw new IllegalStateException("Graph edge weights are not instance of FlowWeight");
		});

		arr.clear();

		do {
			dijkstra(root, e -> 0);
			ArrayList<Node<T>> walk = getWalk(end);
			int min = getMaxFlow(walk);

			if (min != Integer.MAX_VALUE) {
				applyFlow(walk, min, doPrinter);
				flow += min;
			}
		} while (end.time == 0);

		if (doPrinter)
			System.out.println("END ALGORITHM");

		for (Map<Node<T>, Edge<K>> m : edges.values())
			arr.addAll(m.values());

		minCut(root);

		arr.forEach(e -> {
			FlowWeight w = (FlowWeight) e.getWeight();
			if (w.getAvailable() == 0)
				e.setStroke(Color.RED);
			else if (w.flow != 0)
				e.setStroke(Color.BLUE);
			else
				e.setStroke(Color.BLACK);

		});

		return flow;

	}

	private void minCut(Node<T> root) {
		bfs(root);

		System.out.println("\nMIN-CUT:");

		String tmp = "NS = {";
		boolean v = false;

		for (Node<T> n : nodes)
			if (n.time != Integer.MAX_VALUE) {
				v = true;
				tmp += n + ", ";
			}

		if (v)
			tmp = tmp.substring(0, tmp.length() - 2);

		tmp += "}";

		System.out.println(tmp);

		tmp = "NT = {";
		v = false;

		for (Node<T> n : nodes)
			if (n.time == Integer.MAX_VALUE) {
				v = true;
				tmp += n + ", ";
			}

		if (v)
			tmp = tmp.substring(0, tmp.length() - 2);

		tmp += "}";

		System.out.println(tmp);
	}

	@SuppressWarnings("unchecked")
	private void applyFlow(ArrayList<Node<T>> walk, int min, boolean doPrint) {
		for (int i = 0; i < walk.size() - 1; i++) {
			Node<T> from = walk.get(i);
			Node<T> to = walk.get(i + 1);

			Edge<K> edge = edges.get(from).get(to);
			((FlowWeight) edge.getWeight()).flow += min;

			if (((FlowWeight) edge.getWeight()).getAvailable() == 0) {
				if (doPrint)
					System.out.print(edges.get(from).get(to) + " => +" + min + " {FULL} => ");

				removeEdge(from, to);
			} else {

				if (doPrint)
					System.out.print(edges.get(from).get(to) + " => +" + min + " => ");
			}

			FlowWeight f = (FlowWeight) edge.getWeight();
			if (edges.get(to).get(from) == null)
				newEdge(to, from, (K) new FlowWeight(min, f.capacity));
			else
				((FlowWeight) edges.get(to).get(from).getWeight()).flow += min;

		}

		if (doPrint)
			System.out.println("END ITERATION");

	}

	private int getMaxFlow(ArrayList<Node<T>> walk) {
		int min = Integer.MAX_VALUE;

		if (walk.size() == 1)
			return min;

		for (int i = 0; i < walk.size() - 1; i++) {
			int available = ((FlowWeight) edges.get(walk.get(i)).get(walk.get(i + 1)).getWeight()).getAvailable();
			min = (int) Math.min(min, available);
		}

		return min;
	}

	private ArrayList<Node<T>> getWalk(Node<T> end) {
		ArrayList<Node<T>> tmp = new ArrayList<>();

		while (end != null) {
			tmp.add(0, end);
			end = end.parent;
		}

		return tmp;
	}

	@SuppressWarnings("unchecked")
	public final double prim(@NotNull Node<T> root, @NotNull WeightConverter<K> conv) {
		checkThread();
		resetNodes();

		Objects.requireNonNull(root);
		Objects.requireNonNull(conv);

		if (nodes.size() == 0)
			throw new IllegalStateException("prim algorithm cannot be applied to a graph with 0 nodes");

		ArrayList<Node<T>> s = new ArrayList<>(Arrays.asList(root));

		ArrayList<Edge<K>> tree = new ArrayList<>();
		ArrayList<Edge<K>> tmp = new ArrayList<>();

		root.time = 0;

		while (tree.size() != nodes.size() - 1) {
			for (Node<T> n : s)
				tmp.addAll(edges.get(n).values());

			tmp.removeIf(e -> {
				if (e.getNodeTo().time == 0)
					return true;

				for (Edge<K> e2 : tree) {
					if (e.getNodeFrom() == e2.getNodeFrom() && e.getNodeTo() == e2.getNodeTo())
						return true;
					if (e.getNodeFrom() == e2.getNodeTo() && e.getNodeTo() == e2.getNodeFrom())
						return true;
				}

				return false;
			});

			tmp.sort((e1, e2) -> (int) (conv.convert(e1.getWeight()) - conv.convert(e2.getWeight())));

			tree.add(tmp.get(0));
			if (!s.contains(tmp.get(0).getNodeFrom()))
				s.add((Node<T>) tmp.get(0).getNodeFrom());
			if (!s.contains(tmp.get(0).getNodeTo()))
				s.add((Node<T>) tmp.get(0).getNodeTo());

			tmp.get(0).getNodeTo().time = 0;
			tmp.clear();
		}

		tree.forEach(e -> e.setStroke(Color.RED));

		double temp = 0;
		for (Edge<K> e : tree)
			temp += conv.convert(e.getWeight());

		return temp;

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
		resetNodes();

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

	private void resetNodes() {
		for (Node<T> n : nodes) {

			if (edges.get(n) != null)
				edges.get(n).values().forEach(e -> e.setStroke(Color.BLACK));

			n.time = Integer.MAX_VALUE;
			n.parent = null;
		}
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

		edges.put(value, FXCollections.observableMap(new HashMap<>()));

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
		Edge<K> e1 = createEdge(n1, n2, w);

		// create edge n1 <- n2 if it's not a digraph
		if (!digraph) {
			Edge<K> e2 = createEdge(n2, n1, w);
			e1.strokeProperty().addListener((o, old, neww) -> e2.setStroke((Color) neww));
			e2.strokeProperty().addListener((o, old, neww) -> e1.setStroke((Color) neww));
		}

		n1.toFront();
		n2.toFront();

		return this;
	}

	private Edge<K> createEdge(Node<T> n1, Node<T> n2, K weight) {
		// check if edge already exists
		if (edges.get(n1) != null && edges.get(n1).get(n2) != null) {
			edges.get(n1).get(n2).setWeight(weight);
			return edges.get(n1).get(n2);
		}

		Edge<K> arrow = new Edge<K>(n1, n2, n2.getPrefWidth() / 2, weight);
		n2.in.put(n1, arrow);
		n1.out.put(n2, arrow);

		arrow.setStrokeWidth(4);

		arrow.setOnMouseClicked(e -> edgeFocused.set(new GraphTriple<>(n1, arrow, n2)));

		edges.get(n1).put(n2, arrow);

		return arrow;
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

	private final void listenNodeChange(ListChangeListener.Change<? extends Node<?>> c) {
		c.next();
		if (c.wasAdded())
			getChildren().addAll(c.getAddedSubList());

		if (c.wasRemoved())
			getChildren().removeAll(c.getRemoved());
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
