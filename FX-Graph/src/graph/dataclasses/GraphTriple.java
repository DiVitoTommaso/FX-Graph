package graph.dataclasses;

import graph.annotations.Nullable;

public class GraphTriple<P, E, C> {

	private final P parent;
	private final E edge;
	private final C child;

	public GraphTriple(@Nullable P parent, @Nullable E edge, @Nullable C child) {
		this.parent = parent;
		this.edge = edge;
		this.child = child;
	}

	public P getFrom() {
		return parent;
	}

	public E getEdge() {
		return edge;
	}

	public C getTo() {
		return child;
	}
}
