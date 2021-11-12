package graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import graph.dataclasses.FlowWeight;
import graph.gui.FXGraph;
import graph.gui.Node;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;

public class Main extends Application {

	private static Window window;

	public void start(Stage primaryStage) {

		try {
			window = primaryStage;

			Node<String>[] nodes = Node.array("1", "2", "3", "4", "5");
			HashMap<String, Node<String>> map = Node.asMap(nodes);

			FXGraph<String, FlowWeight> fotocopiatrici = new FXGraph<>(true);

			fotocopiatrici.addNodes(nodes);
			fotocopiatrici.newEdge(map.get("3"), map.get("1"), new FlowWeight(0, 4));
			fotocopiatrici.newEdge(map.get("5"), map.get("1"), new FlowWeight(0, 3));
			fotocopiatrici.newEdge(map.get("2"), map.get("1"), new FlowWeight(0, 1));
			fotocopiatrici.newEdge(map.get("3"), map.get("2"), new FlowWeight(0, 1));
			fotocopiatrici.newEdge(map.get("4"), map.get("3"), new FlowWeight(0, 4));
			fotocopiatrici.newEdge(map.get("2"), map.get("4"), new FlowWeight(0, 2));
			fotocopiatrici.newEdge(map.get("5"), map.get("2"), new FlowWeight(0, 2));
			fotocopiatrici.newEdge(map.get("5"), map.get("4"), new FlowWeight(0, 3));

			Map<Node<String>, Integer> ex = new HashMap<>();
			Map<Node<String>, Integer> dx = new HashMap<>();
			ex.put(map.get("3"), 2);
			ex.put(map.get("5"), 2);

			dx.put(map.get("4"), -1);
			dx.put(map.get("1"), -3);

			System.out.println("Min cost: " + fotocopiatrici.edmondsKarpLogged(map.get("5"), map.get("1")));

			primaryStage.setScene(new Scene(fotocopiatrici, 1280, 720));
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
			Platform.exit();
		}

	}

	public static void main(String[] args) {
		launch(args);
	}

	public static Window window() {
		return window;
	}

}