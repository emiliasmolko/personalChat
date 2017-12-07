package service;

import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import com.ibm.watson.developer_cloud.conversation.v1.model.Context;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;



@Path("/")
public class ChatService {

	private static final String CLASS_NAME = ChatService.class.getName();
	private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

	@Inject
	private WatsonConversation conversation;
	private Client translateService;
	private final String[] zlote = {"(?i)\\s?z(ł|l)oty(ch)?('.'|' ')?","(?i)\\s?z(ł|l)ote('.'|' ')?", "(?i)\\s?z(ł|l)('.'|' ')?"};
	private final String PLN = " PLN ";
	private final String[] grosze = {" cent ", " cents ", "(?i)\\s?groszy('.'|' ')?", "(?i)\\s?(gr('.')?)(?!\\S)('.')?", " crore "};
	private final String GROSZE = " grosze ";

	@PostConstruct
	private void postConstruct() {
		translateService = ClientBuilder.newClient();
	}

	private String translate(String text) {
		boolean isPLN = false;
		String message = replace(" "+text+" ", zlote, PLN);
		if(message.indexOf(PLN)>=0) {
			isPLN = true;
		}
		LOGGER.info("przed translate:"+message);
		message = translateService.target("https://translate.googleapis.com/translate_a/single").queryParam("client", "gtx").queryParam("dt", "t")
		        .queryParam("sl", "pl").queryParam("tl", "en").queryParam("ie", "UTF-8").queryParam("oe", "UTF-8")
		        .queryParam("q", message)
		        .request(MediaType.APPLICATION_JSON)
		        .get(String.class);		
		LOGGER.info("po translate:"+message);
		message = message.substring(4, message.indexOf("\",\"", 4));
		
		if(isPLN) {
			message = replace(" "+message+" ", grosze, GROSZE);
		}
		return message;
	}
	private String replace(String source, String[] array, String target) {
		LOGGER.info("przed replace:"+source);
		String text = source;
		for(int i=0; i<array.length; i++) {
			text = text.replaceAll(array[i], target);
		}
		LOGGER.info("po replace:"+text);
		return text;
	}
	@POST
	@Path("/message/{cn}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public List<String> runMessage(@PathParam("cn")String conversationId, String text) {
		MessageResponse mr = conversation.message(translate(text), conversationId);
		LOGGER.info("runMessage"+mr);	
		List<String> result = mr.getOutput().getText();
		result.add(0, mr.getContext().get("conversation_id").toString());
		return result;
	}
	@GET
	@Path("/context/{cn}")
	@Produces(MediaType.APPLICATION_JSON)
	public Context getContext(@PathParam("cn")String conversationId) {
		return conversation.getContext(conversationId);
	}


}
