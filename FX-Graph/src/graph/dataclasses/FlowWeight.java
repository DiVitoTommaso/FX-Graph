package graph.dataclasses;

public class FlowWeight {

	public int flow;
	public int capacity;

	public FlowWeight(int flow, int capacity) {
		super();
		this.flow = flow;
		this.capacity = capacity;
	}

	public int getAvailable() {
		return capacity - flow;
	}

	@Override
	public String toString() {
		return "[" + flow + "," + capacity + "]";
	}

}
