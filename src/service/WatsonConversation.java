package service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import com.ibm.watson.developer_cloud.conversation.v1.Conversation;
import com.ibm.watson.developer_cloud.conversation.v1.model.Context;
import com.ibm.watson.developer_cloud.conversation.v1.model.InputData;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageOptions;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;

@Stateless
@LocalBean
public class WatsonConversation {
	private String workspaceId = "";
	private Conversation service = null;
	private MessageOptions options = null;
	private static HashMap<String, Context> mem = new HashMap<String, Context>();
	
	public MessageResponse message(String input, String conversationId) {
		InputData data = new InputData.Builder(input).build();
		Context context = getContext(conversationId);
		if(context == null){
			context = new Context();
			context.put("currency", "PLN");
			context.put("timezone", "Europe/Warsaw");
		}
		options = options.newBuilder().input(data).context(context).build();
		MessageResponse response = service.message(options).execute();
		context = response.getContext();
		
		mem.put(context.get("conversation_id").toString(),context);
		return response;
	}
	public Context getContext(String conversationId) {
		return mem.get(conversationId);
	}
	@PostConstruct
	public void init() {
		Properties properties = new Properties();
		InputStream inputStream = getClass().getResourceAsStream("/META-INF/service.properties");
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			System.out.println("Can't load service.properties file");
		}
		service = new Conversation(Conversation.VERSION_DATE_2017_05_26);
		service.setUsernameAndPassword(properties.getProperty("ConversationUsername"), properties.getProperty("ConversationPassword"));
		workspaceId = properties.getProperty("ConversationWorkspaceId");
		options = new MessageOptions.Builder(workspaceId).build();		
	}

}