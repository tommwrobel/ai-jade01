package jadelab1;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MyAgent extends Agent {
	protected void setup () {
		displayResponse("Hello, I am " + getAID().getLocalName());
		addBehaviour(new MyCyclicBehaviour(this));
		//doDelete();
	}
	protected void takeDown() {
		displayResponse("See you");
	}
	public void displayResponse(String message) {
		JOptionPane.showMessageDialog(null,message,"Message",JOptionPane.PLAIN_MESSAGE);
	}
	public void displayHtmlResponse(String html, String word) {
		JTextPane tp = new JTextPane();
		JScrollPane js = new JScrollPane();
		js.getViewport().add(tp);
		JFrame jf = new JFrame();
		jf.getContentPane().add(js);
		jf.pack();
		jf.setSize(400,500);
		jf.setVisible(true);
		tp.setContentType("text/html");
		tp.setEditable(false);
		jf.setTitle("In response to: " + word);
		tp.setText(html);
	}
}

class MyCyclicBehaviour extends CyclicBehaviour {
	MyAgent myAgent;
	Map<String, String> messages = new HashMap<>();
	public MyCyclicBehaviour(MyAgent myAgent) {
		this.myAgent = myAgent;
	}
	public void action() {
		ACLMessage message = myAgent.receive();
		if (message == null) {
			block();
		} else {
			String ontology = message.getOntology();
			String content = message.getContent();
			String id = message.getInReplyTo();
			int performative = message.getPerformative();
			if (performative == ACLMessage.REQUEST)
			{
				//I cannot answer but I will search for someone who can
				DFAgentDescription dfad = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setName("dictionary");
				dfad.addServices(sd);
				try
				{
					DFAgentDescription[] result = DFService.search(myAgent, dfad);
					if (result.length == 0) myAgent.displayResponse("No service has been found ...");
					else
					{
						// if agent exists, create uuid for message and send message to agent
						String messageId = UUID.randomUUID().toString();
						messages.put(messageId, message.getContent());

						String foundAgent = result[0].getName().getLocalName();
						myAgent.displayResponse("Agent " + foundAgent + " is a service provider. Sending message to " + foundAgent);

						ACLMessage forward = new ACLMessage(ACLMessage.REQUEST);
						forward.addReceiver(new AID(foundAgent, AID.ISLOCALNAME));
						forward.setContent(content);
						forward.setOntology(ontology);
						forward.setReplyWith(messageId);
						myAgent.send(forward);
					}
				}
				catch (FIPAException ex)
				{
					ex.printStackTrace();
					myAgent.displayResponse("Problem occurred while searching for a service ...");
				}
			}
			else
			{	//when it is an answer
				String word = messages.get(id);
				myAgent.displayHtmlResponse(content, word);
			}
		}
	}
}
