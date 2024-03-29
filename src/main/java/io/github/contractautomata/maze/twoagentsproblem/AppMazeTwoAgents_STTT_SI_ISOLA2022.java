package io.github.contractautomata.maze.twoagentsproblem;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.label.action.IdleAction;
import io.github.contractautomata.catlib.automaton.label.action.OfferAction;
import io.github.contractautomata.catlib.automaton.label.action.RequestAction;
import io.github.contractautomata.catlib.automaton.state.BasicState;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition.Modality;
import io.github.contractautomata.catlib.converters.AutConverter;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomata.catlib.operations.*;
import io.github.contractautomata.catlib.requirements.Agreement;
import io.github.contractautomata.maze.converters.JSonConverter;
import io.github.contractautomata.maze.converters.PngConverter;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AppMazeTwoAgents_STTT_SI_ISOLA2022
{

	//by default the set-up is the the first experiment;
	private static String gateCoordinates = "(2; 7; 0)";
	private static String agent1coordinates = "(1; 1; 0)";
	private static String agent2coordinates = "(1; 2; 0)";
	private static String path_to_image = System.getProperty("user.dir")+"/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/maze3.png";

	private static String path_to_voxlogica_output = System.getProperty("user.dir")+"/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/experimentsSTTT_1.json";
	private static String outputCompositionPath = System.getProperty("user.dir")+"/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/composition_1.data";
	private static String inputCompositionPath = System.getProperty("user.dir")+"/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/composition_1.data";

	private static String forbiddenAttribute = "";
	private static String finalAttribute = "";
	private static String controllability = "1";
	private final static PngConverter pdc = new PngConverter();
	private final static AutDataConverter<CALabel> dc = new AutDataConverter<>(CALabel::new);

	private static Modality necessary = Modality.URGENT;

	public static void main( String[] args ) throws Exception {

		String message = "Usage : java -jar maze-0.0.2-SNAPSHOT-jar-with-dependencies.jar [options] \n" +
				"where options are : \n" +
				"-phase1 (compute the composition and generate the images) \n" +
				"-phase2 (read the logs of voxlogica and perform the synthesis) \n"+
				"-experiment [1|2|3] (select the set-up of either experiment 1, 2 or 3)\n" +
				"-gateCoordinates x y \n" +
				"-position_agent_1 x y  \n" +
				"-position_agent_2 x y \n" +
				"-imagePath String (the path where the image is located) \n" +
				"-outputCompositionPath String (the path where to store the composition) \n"+
				"-inputCompositionPath String (the path where the composition has been stored) \n"+
				"-voxLogica_output_path String (the path where the output of VoxLogica is located) \n"+
				"-forbiddenAttribute String (name of the forbidden attribute in the voxlogica log) \n"+
				"-finalAttribute String (name of the final attribute in the voxlogica log) \n"+
				"-controllability [1|2]  (1: agents controllable, gate uncontrollable - 2: gate controllable, agents uncontrollable)";

		boolean phase1=false;
		boolean phase2=false;
		String exp="";
		Instant start, stop;
		long elapsedTime;


		System.out.println( "Maze example with two agents." );


		if (args==null || args.length==0) {
			System.out.println(message);
			return;
		}

		label:
		for(int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-gateCoordinates":
					gateCoordinates = "(" + args[++i] + "; " + args[++i] + "; 0)";
					break label;
				case "-position_agent_1":
					agent1coordinates = "(" + args[++i] + "; " + args[++i] + "; 0)";
					break label;
				case "-position_agent_2":
					agent2coordinates = "(" + args[++i] + "; " + args[++i] + "; 0)";
					break label;
				case "-imagePath":
					path_to_image = args[++i];
					break label;
				case "-voxLogica_output_path":
					path_to_voxlogica_output = args[++i];
					break label;
				case "-outputCompositionPath":
					outputCompositionPath = args[++i];
					break label;
				case "-inputCompositionPath":
					inputCompositionPath = args[++i];
					break label;
				case "-phase1":
					phase1 = true;
					break;
				case "-phase2":
					phase2 = true;
					break;
				case "-experiment":
					exp = args[++i];
					break;
				case "-forbiddenAttribute":
					forbiddenAttribute = args[++i];
					break;
				case "-finalAttribute":
					finalAttribute = args[++i];
					break;
				case "-controllability":
					controllability = args[++i];
					break;
			}
		}

		if (exp!="") {
			path_to_voxlogica_output = System.getProperty("user.dir") + "/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/experimentsSTTT_"+exp+".json";
			outputCompositionPath = System.getProperty("user.dir")+"/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/composition_"+exp+".data";
			inputCompositionPath = System.getProperty("user.dir")+"/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/composition_"+exp+".data";

			if (exp.equals("1")) //first experiment: agents controllable, gate uncontrollable
				controllability = "1";
			else if (exp.equals("2") || exp.equals("3"))
				controllability = "2"; //second and third experiment: agents uncontrollable, gate controllable

			if (exp.equals("3")) {
				agent1coordinates = "(0; 4; 0)";
				agent2coordinates = "(2; 4; 0)";
				gateCoordinates = "(4; 4; 0)";
				path_to_image = System.getProperty("user.dir") + "/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/trainExample.png";
				necessary = Modality.LAZY;
			}

			if (forbiddenAttribute == "" || finalAttribute == "") {
				forbiddenAttribute = "forbidden" + exp;
				finalAttribute = "final" + exp;
			}
		}

		System.out.println("The set-up is:" +"\n"+
				"experiment="+exp+"\n"+
				"gateCoordinates="+gateCoordinates+"\n"+
				"position_agent_1="+agent1coordinates+"\n"+
				"position_agent_2="+agent2coordinates+"\n"+
				"imagePath="+path_to_image+"\n"+
				"outputCompositionPath="+outputCompositionPath+"\n"+
				"inputCompositionPath="+inputCompositionPath+"\n"+
				"voxLogica_output_path="+path_to_voxlogica_output+"\n"+
				"forbiddenAttribute="+forbiddenAttribute+"\n"+
				"finalAttribute="+finalAttribute+"\n"+
				"controllability="+controllability+"\n"+
				"necessary="+necessary+"\n");



		if (phase1) {
			start = Instant.now();
			System.out.println("Computing composition..." + start);
			Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> comp = computesComposition(exp);
			stop = Instant.now();
			elapsedTime = Duration.between(start, stop).toMillis();
			System.out.println("Elapsed time " + elapsedTime + " ms");

			start = Instant.now();
			System.out.println("Start generating images at "+start);
			generateImagesForEachState(comp);
			stop = Instant.now();
			elapsedTime = Duration.between(start, stop).toMillis();
			System.out.println("Elapsed time "+elapsedTime+ " ms");


			System.out.println("Exporting the composition");
			dc.exportMSCA(outputCompositionPath, comp);
		}
		if (phase2) {

			start = Instant.now();
			System.out.println("Start marking automaton with voxlogica output at " + start);
			System.out.println("Importing composition ");
			Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> comp = dc.importMSCA(inputCompositionPath);//loadFile("composition.data", false);
			Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> marked = readVoxLogicaOutputAndMarkStates(comp,exp); //computing

			stop = Instant.now();
			elapsedTime = Duration.between(start, stop).toMillis();
			System.out.println("Elapsed time " + elapsedTime + " ms");

			if (exp!="")
				dc.exportMSCA("marked_"+exp,marked);
//			marked.getTransition().parallelStream()
//					.filter(t->t.isNecessary()&&t.getLabel().isOffer())
//							.forEach(System.out::println);

			SynthesisOperator sos;
			if (exp.equals("3")) {
				sos = new OrchestrationSynthesisOperator<String>(new Agreement());
				OrchestrationSynthesisOperator.setReachabilityLazy();
			}
			else
				sos = new MpcSynthesisOperator(new Agreement());


			System.out.println("Start computing the synthesis at "+start);
			Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> strategy =
					sos.apply(marked);

			stop = Instant.now();
			elapsedTime = Duration.between(start, stop).toMillis();
			System.out.println("Elapsed time "+elapsedTime+ " ms");

			if (strategy!=null) {
				System.out.println("Exporting the strategy");
				dc.exportMSCA("strategy_"+exp, strategy);
			} else {
				System.out.println("The strategy is empty.");
			}
		}
	}


	private static Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> computesComposition(String exp) throws IOException{

		//compute the composition of two agents and a driver and export it
		System.out.println("create driver and door...");

		final State<String> stateDriver = new State<>(List.of(new BasicState<>("Driver",true,true)));
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> driver =
				new Automaton<>((exp.equals("3")?Stream.of("goup","godown","goright")
						:Stream.of("goup","godown","goleft","goright"))
						.map(s->new CALabel(1,0,new OfferAction(s)))
						.map(act->new ModalTransition<>(stateDriver,act,stateDriver,Modality.PERMITTED))
						.collect(Collectors.toSet()));

		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> door =
				new Automaton<>(Map.of(new State<>(List.of(new BasicState<>("Close",true,false))),
								new State<>(List.of(new BasicState<>("Open",false,true))))
						.entrySet().stream()
						.flatMap(e->Stream.of(new ModalTransition<>(e.getKey(), new CALabel(1,0,new OfferAction("open")), e.getValue(), Modality.PERMITTED),
								new ModalTransition<>(e.getValue(), new CALabel(1,0,new OfferAction("close")), e.getKey(), Modality.PERMITTED)))
						.collect(Collectors.toSet()));

		System.out.println("importing the maze image");

		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>  maze = pdc.importMSCA(path_to_image);

		//relabel bifunction is used to set the initial state of the two agents
		//whose set of transitions is called here maze_tr and maze2_tr.
		//Basically, they only differ in the initial state, whilst the final
		//states are all cells with colour #000000.
		//However, when marking the composition, the initial states will be loaded from VoxLogicA.
		BiFunction<String, Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>,
				Set<ModalTransition<String,Action,State<String>,CALabel>>> relabel = (l,aut) ->
				new RelabelingOperator<String,CALabel>(CALabel::new,s->s,s->s.getState().split("_")[0].equals(l),
						s->!s.getState().contains("#000000")).apply(aut);

		Set<ModalTransition<String,Action,State<String>,CALabel>> maze_tr = relabel.apply(agent1coordinates,maze);
		Set<ModalTransition<String,Action,State<String>,CALabel>> maze2_tr = relabel.apply(agent2coordinates,maze);

		Predicate<State<String>> badState = s->
				//two agents on the same cell
				s.getState().get(0).getState().split("_")[0].equals(s.getState().get(1).getState().split("_")[0])
						||
						IntStream.range(0,2)
								.mapToObj(i->s.getState().get(i).getState())
								.anyMatch(l->(l.contains("#000000")) //one agent on black
										||
										(l.contains(gateCoordinates) && s.getState().get(3).getState().contains("Close")));
		//one agent on the gate whilst the gate is closed

		System.out.println("composing...");

		MSCACompositionFunction<String> cf = new MSCACompositionFunction<>(List.of(new Automaton<>(maze_tr),
				new Automaton<>(maze2_tr),driver,door),t->t.getLabel().isRequest() || (badState.test(t.getTarget())));

		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>  comp =
				cf.apply(Integer.MAX_VALUE);

		return comp;
	}

	/**
	 * this private method takes the composition automaton, where each state of the composition
	 * is a tuple of the position of each agent and the door state, and generates for each state of the composition a json encoding image
	 * of GraphLogica or a png where in the original starting image the positions of the two agents are emphasised with two
	 * different colors (red and green) whilst the door is blue
	 *
	 */
	private static void generateImagesForEachState(
			Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut) throws Exception {

		System.out.println("Generating images...");

		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> maze = pdc.importMSCA(path_to_image);

		aut.getStates().parallelStream()
				.forEach(aut_s->{
					final String s1 = aut_s.getState().get(0).getState().split("_")[0];
					final String s2 = aut_s.getState().get(1).getState().split("_")[0];
					final boolean gateClosed = aut_s.getState().get(3).getState().equals("Close");

					Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>
							mazewithagents = new Automaton<>(new RelabelingOperator<String,CALabel>(CALabel::new, s->
							gateClosed&&s.split("_")[0].equals(gateCoordinates)?gateCoordinates+"_#0000FF": //if the gate is closed it must be draw first
									s.split("_")[0].equals(s1)?s.split("_")[0]+"_#FF0000"
											:s.split("_")[0].equals(s2)?s.split("_")[0]+"_#00FF00"
											:s, BasicState::isInitial, BasicState::isFinalState)
							.apply(maze));
					try {
						AutConverter<?,
								Automaton<String,Action,State<String>,ModalTransition<String,Action,State<String>,CALabel>>> ac = pdc; //json?jdc:pdc;
						new File("./png").mkdirs();
						ac.exportMSCA("./png/"+JSonConverter.getstate.apply(aut_s), mazewithagents);
					} catch (Exception e) {
						RuntimeException re = new RuntimeException();
						re.addSuppressed(e);
						throw re;
					}
				});
	}


	private static Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> readVoxLogicaOutputAndMarkStates(Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut,
																																				   String exp) throws IOException {

		System.out.println("reading voxlogica computed file");

		//parse the voxlogica json output and extract the information about initial, final and forbidden states
		String content =Files.readAllLines(Path.of(path_to_voxlogica_output))
				.stream()
				.collect(Collectors.joining(" "));

		JSONArray obj = new JSONArray(content);

		final Set<String> initialstate = Set.of(agent1coordinates.replaceAll(";", ",")+","+agent2coordinates.replaceAll(";", ",")+",Driver,Close");


		final Set<String> finalstates = extractFromJSON(obj, finalAttribute);
		final Set<String> forbiddenstates = extractFromJSON(obj, forbiddenAttribute);

//		final Set<String> finalstatesC = markStatesExperiment3(aut,true);
//		final Set<String> forbiddenstatesC = markStatesExperiment3(aut,false);

		//		System.out.println("The final states are:"+finalstatesC);
//		System.out.println("The final states marked by the JSON are:"+finalstates);
//		System.out.println(finalstates.size());
	//	System.out.println(forbiddenstatesC.size());

//		System.out.println(finalstates.equals(finalstatesC));
//		System.out.println(forbiddenstates.equals(forbiddenstatesC));
//		System.out.println("The forbidden states are:"+forbiddenstatesC);
//		System.out.println("The forbidden states marked by the JSON are:"+forbiddenstates);

//
//		System.out.println("Stati che sono forbidden nel JSON ma non dovrebbero esserlo: \n"+
//				forbiddenstates.parallelStream()
//						.filter(s->!forbiddenstatesC.contains(s))
//										.collect(Collectors.joining("\n")));
//
//		System.out.println("Stati che sono forbidden ma non lo sono nel JSON: \n"+
//				forbiddenstatesC.parallelStream()
//						.filter(s->!forbiddenstates.contains(s))
//						.collect(Collectors.joining("\n")));
//


		finalstates.removeAll(forbiddenstates); //final states cannot be forbidden


		System.out.println("Updating the automaton");

		//reset initial and final states to false
		RelabelingOperator<String,CALabel> ro = new RelabelingOperator<>(CALabel::new,x->x,x->false,x->false);

		Modality agentModality = (controllability.equals("1"))?Modality.PERMITTED:necessary; //experiments 1
		Modality gateModality =  (controllability.equals("2"))?Modality.PERMITTED:necessary; //experiments 2 and 3
		System.out.println("Reset initial and final states, and selected agents to uncontrollable");

		//turn the moves of the opponent to uncontrollable
		Set<ModalTransition<String, Action, State<String>, CALabel>> setr = ro.apply(aut).parallelStream()
				.map(t->new ModalTransition<>(t.getSource(),t.getLabel(),t.getTarget(),
						(t.getLabel().getContent().get(3) instanceof IdleAction)&&t.getLabel().isMatch()?agentModality
								:gateModality
				)).collect(Collectors.toSet());


		System.out.println("State Marking ... ");

		//mark initial, final and forbidden states

		//firstly, two new states, initial and final, are generated.
		BasicState<String> bsi = new BasicState<>("Init",true,false);
		final State<String> init = new State<>(List.of(bsi,bsi,bsi,bsi));

		final State<String> finalstate = new State<>(IntStream.range(0,4)
				.mapToObj(i->new BasicState<>("Final",false,true))
				.collect(Collectors.toList()));

		// on forbidden states a self loop is added with a urgent request
		Function<State<String>, ModalTransition<String, Action, State<String>, CALabel>> f_forbidden =
				(State<String> s)->new ModalTransition<>(s,new CALabel(s.getRank(),0,new RequestAction("forbidden")),s,Modality.URGENT);


		BiPredicate<State<String>,Set<String>> pred =  (s,set) -> set.parallelStream()
				//		.peek(x->{if (x.equals(initialstate.iterator().next()))  System.out.println(x+" ---- " + JSonConverter.getstate.apply(s));})
				.anyMatch(x->x.equals(JSonConverter.getstate.apply(s)));


		//we add to the transitions those outgoing from "Init" to the initial state, and from the final states to "Final"
		setr.addAll(Map.of(initialstate,(State<String> s)-> new ModalTransition<>(init,new CALabel(s.getRank(),0,new OfferAction("start")),s,Modality.PERMITTED),
						finalstates,(State<String> s)->new ModalTransition<>(s, new CALabel(s.getRank(),0,new OfferAction("stop")),	finalstate,Modality.PERMITTED),
						forbiddenstates,f_forbidden)
				.entrySet().parallelStream()
				.flatMap(e->setr.parallelStream()
						.flatMap(t->Stream.of(t.getSource(),t.getTarget()))
						.filter(s->pred.test(s,e.getKey()))
						.map(e.getValue()))
				.collect(Collectors.toSet()));

		return new Automaton<>(setr);
	}


	private static Set<String> extractFromJSON(JSONArray obj, String key){
		return IntStream.range(0,obj.length())
				.mapToObj(obj::getJSONObject)
				.map(o->o.getJSONObject("results"))
				.flatMap(o->o.toMap().entrySet().parallelStream()
						.filter(e->e.getKey().contains(key))
						.filter(e->(Boolean) e.getValue())
						.map(e->e.getKey().split("_")[1].split(".png")[0]))
				.collect(Collectors.toSet());
	}

	private static void test() throws IOException {
		Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> older = dc.importMSCA(System.getProperty("user.dir")+"/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/twoagents_maze_marked_firstexp.data");
		Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> newer = dc.importMSCA(System.getProperty("user.dir")+"/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/twoagents_maze_marked_firstexp_new.data");

		System.out.println(older.getTransition().stream()
				.filter(t->t.getTarget().isFinalState())
				.count());
		System.out.println(newer.getTransition().stream()
				.filter(t->t.getTarget().isFinalState())
				.count());
		if (true)
			return;
	}


	private static Set<String> markStatesExperiment3(Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut, boolean fin){
		final Function<State<String>, Boolean> checkX = s -> {
			int x_1 = Integer.parseInt(s.getState().get(0).getState().split(";")[0].substring(1));
			int x_2 = Integer.parseInt(s.getState().get(1).getState().split(";")[0].substring(1));
			boolean gateClosed = s.getState().get(3).getState().contains("Close");
			boolean bothTrainsInsideJunction = x_1>4 && x_1 <9 && x_2>4 && x_2<9;
			boolean openedGate = (x_1>0 && x_1<4) &&  //first train before the semaphore
					x_2>4 && x_2<9 && // second train inside the junction area
					!gateClosed; //the semaphore is green

			return (fin)?x_1>=9 && x_2 >=9    //final states
					:bothTrainsInsideJunction || openedGate ; //forbidden states
		};
		return aut.getStates().parallelStream()
				.filter(s->checkX.apply(s))
				.map(JSonConverter.getstate::apply)
				.collect(Collectors.toSet());
	}
}