package graph;

import java.util.HashMap;

import graph.gui.FXGraph;
import graph.gui.Node;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

public class Main extends Application {

	private static Window window;

	public void start(Stage primaryStage) {

		try {
			window = primaryStage;

			Node<String>[] nodes = Node.array("INIZIO", "INF", "ELET", "MECC", "CIV", "SPA", "GER", "FR", "NOR", "GB", "FINE");
			HashMap<String, Node<String>> map = Node.asMap(nodes);
			
			FXGraph<String, Integer> fotocopiatrici = new FXGraph<>(true);
			
			fotocopiatrici.addNodes(nodes);
			fotocopiatrici.newEdge(map.get("INIZIO"), map.get("INF"), 20);
			fotocopiatrici.newEdge(map.get("INIZIO"), map.get("ELET"), 16);
			fotocopiatrici.newEdge(map.get("INIZIO"), map.get("MECC"), 15);
			fotocopiatrici.newEdge(map.get("INIZIO"), map.get("CIV"), 20);
			fotocopiatrici.newEdge(map.get("INF"), map.get("SPA"), 10);
			fotocopiatrici.newEdge(map.get("INF"), map.get("GER"), 10);
			fotocopiatrici.newEdge(map.get("INF"), map.get("NOR"), 10);
			fotocopiatrici.newEdge(map.get("INF"), map.get("GB"), 10);
			
			fotocopiatrici.newEdge(map.get("ELET"), map.get("GER"), 10);
			fotocopiatrici.newEdge(map.get("ELET"), map.get("NOR"), 10);
			fotocopiatrici.newEdge(map.get("ELET"), map.get("GB"), 10);
			fotocopiatrici.newEdge(map.get("ELET"), map.get("FR"), 10);
			
			fotocopiatrici.newEdge(map.get("MECC"), map.get("GER"), 10);
			fotocopiatrici.newEdge(map.get("MECC"), map.get("SPA"), 10);
			fotocopiatrici.newEdge(map.get("MECC"), map.get("GB"), 10);
			fotocopiatrici.newEdge(map.get("MECC"), map.get("FR"), 10);
			
			fotocopiatrici.newEdge(map.get("CIV"), map.get("SPA"), 10);
			fotocopiatrici.newEdge(map.get("CIV"), map.get("NOR"), 10);
			fotocopiatrici.newEdge(map.get("CIV"), map.get("GB"), 10);
			fotocopiatrici.newEdge(map.get("CIV"), map.get("FR"), 10);
			
			fotocopiatrici.newEdge(map.get("GER"), map.get("FINE"), 20);
			fotocopiatrici.newEdge(map.get("SPA"), map.get("FINE"), 20);
			fotocopiatrici.newEdge(map.get("FR"), map.get("FINE"), 25);
			fotocopiatrici.newEdge(map.get("NOR"), map.get("FINE"), 5);
			fotocopiatrici.newEdge(map.get("GB"), map.get("FINE"), 15);
			
			
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