package graph;

import java.util.HashMap;
import java.util.Map;

import graph.dataclasses.FlowWeight;
import graph.gui.FXGraph;
import graph.gui.Node;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	public void start(Stage primaryStage) {

		try {
			Node<String>[] nodes = Node.array("1", "2", "3", "4");
			HashMap<String, Node<String>> map = Node.asMap(nodes);
			Node.shuffle(new Rectangle2D(0, 0, 1280, 720), nodes);

			FXGraph<String, FlowWeight> min = new FXGraph<>(true);

			min.addNodes(nodes);
			min.addEdge(map.get("3"), map.get("1"), new FlowWeight(-5, 2));
			min.addEdge(map.get("1"), map.get("2"), new FlowWeight(2, 3));
			min.addEdge(map.get("4"), map.get("1"), new FlowWeight(2, 2));
			min.addEdge(map.get("4"), map.get("3"), new FlowWeight(3, 1));
			min.addEdge(map.get("3"), map.get("4"), new FlowWeight(-3, 3));
			min.addEdge(map.get("2"), map.get("4"), new FlowWeight(1, 1));
			min.addEdge(map.get("4"), map.get("2"), new FlowWeight(-1, 1));

			Map<Node<String>, Integer> ex = new HashMap<>();
			Map<Node<String>, Integer> dx = new HashMap<>();
			ex.put(map.get("3"), 1);
			dx.put(map.get("1"), -1);

			System.out.println("Min cost: " + min.minFlowLogged(ex, dx));

			primaryStage.setScene(new Scene(min, 1280, 720));
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
			Platform.exit();
		}

	}

	public static void main(String[] args) {
		launch(args);
	}

}