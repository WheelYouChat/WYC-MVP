	package com.wyc.viber;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.wyc.WYCConfig;
import com.wyc.annotation.BotMethod;
import com.wyc.db.model.Person;
import com.wyc.service.MenuService;
import com.wyc.viber.ViberButton.ViberButtonActionType;
import com.wyc.viber.ViberButtonActionBody.Event;
import com.wyc.viber.ViberKeyBoard.ViberKeyBoardType;
import com.wyc.viber.ViberMessage.ViberMessageType;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
@Slf4j
public class ViberApi {
	@Autowired
	private WYCConfig config;
	
	@Autowired
	private MenuService menuService;
	
	private OkHttpClient client = new OkHttpClient();
	private boolean initialized = false;

	@Data	
	public static class WebHook {
		private String url;
		private String[] event_types;
	}
	
	public void setWebHook() throws IOException {
		
		if(!initialized) {
			String webhookUrl = config.getViber().getWebHookUrl();
			log.debug(webhookUrl);
	
			 String json = "{\"url\":\"" + webhookUrl + "\", \"event_types\":[\"delivered\",\"seen\",\"failed\",\"subscribed\",\"unsubscribed\",\"conversation_started\"]}";
			 RequestBody body = RequestBody.create(MediaType.parse("text/json"), json);
			 Response response = doRequest("/pa/set_webhook", body);
			 String r = response.body().string();
			 log.info(r);
			 
			 ViberButton[] buttons = new ViberButton[] {
					 ViberButton
					 	.builder()
					 	.BgColor("#FF0000")
					 	.ActionType(ViberButtonActionType.reply)
					 	.ActionBody("Do something")
					 	.Columns(3)
					 	.Rows(1)
					 	.Text("Hello")
					 	.build(),
				 	ViberButton
					 	.builder()
					 	.BgColor("#0000FF")
					 	.ActionType(ViberButtonActionType.reply)
					 	.ActionBody("Do something 2")
					 	.Columns(3)
					 	.Rows(1)
					 	.Text("Good bye")
					 	.build(),
					 
			 };
			ViberKeyBoard keyboard = ViberKeyBoard.builder()
					.Type(ViberKeyBoardType.keyboard)
					.DefaultHeight(true)
					.Buttons(buttons)
					.build();
			// sendMessage("sSPFZVqFK9BNhd4qFve6Rw==", "Hello 2", keyboard);

			 log.info(r);
			 initialized = true;
		}
	}
	
	protected Response doRequest(String url, RequestBody body) throws IOException {
		 String fullUrl = config.getViber().getViberBaseUrl() + url;

		 Request request = new Request.Builder()
			        .url(fullUrl)
			        .addHeader("X-Viber-Auth-Token", config.getViber().getToken())
			        .post(body)
			        .build();
		 Call call = client.newCall(request);
		 Response response = call.execute();
		 return response;
	}
	
	public ViberSentMessage sendMessage(String to, ViberMessage msg, String from) throws IOException {
		return sendMessage(to, msg.getText(), msg.getKeyboard(), msg.getType(), from);
	}
	
	public ViberSentMessage sendMessage(String to, String msg, String from) throws IOException {
		return sendMessage(to, msg, null, null, from);
	}
	
	public ViberSentMessage sendMessage(String to, String msg, ViberKeyBoard keyboard, ViberMessageType type, String from) throws IOException {
		if(type == null) {
			type = msg == null ? null : ViberMessageType.text;
		}
		if(keyboard != null && (keyboard.getButtons() == null || keyboard.getButtons().length == 0)) {
			keyboard = null;
		}
		ViberMessage message = ViberMessage.builder()
				.receiver(to)
				.text(msg)
				.type(type)
				.sender(ViberSender.builder().name(from).build())
				.keyboard(keyboard)
				.build();
		
		
		ObjectMapper mapper = createObjectMapper();
		String json = mapper.writeValueAsString(message);  
				 
		RequestBody body = RequestBody.create(MediaType.parse("text/json"), json);
		log.info("Sending " + json);
		Response response = doRequest("/pa/send_message", body);
		String r = response.body().string();
		log.info(r);
		ViberSentMessage res = mapper.readValue(r, ViberSentMessage.class);
		return res;
	}
	
	protected ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		PropertyNamingStrategy strategy = new PropertyNamingStrategy() {

			@Override
			public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
				Method m = method.getAnnotated();
				Class<?> cls = m.getDeclaringClass();
				try {
					String fName = defaultName.substring(0, 1).toUpperCase() + defaultName.substring(1);
					Field[] allFields = cls.getDeclaredFields();
					log.debug("" + allFields);
					Field f = cls.getDeclaredField(fName);
					log.info("Found field : " + f);
					return fName;
					
				} catch (NoSuchFieldException e) {
					// Field is not found
				} catch (SecurityException e) {
					log.error("Error", e);
				}
				
				return super.nameForGetterMethod(config, method, defaultName);
			}
		};

		mapper.setPropertyNamingStrategy(strategy );
		return mapper;
	}
	
	public ViberIncomingMessage parseIncommingMessage(String json) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = createObjectMapper();
		ViberIncomingMessage res = mapper.readValue(json, ViberIncomingMessage.class);
		return res;
	}

	public ViberKeyBoard createMenuBackToMainMenu() {
		List<Pair<String, Method>> menuMethods = menuService.getBackToMenuMethods();
		return createMenu(menuMethods);
	}
	
	public ViberKeyBoard createMainMenu(Person.Role role) {
		List<Pair<String, Method>> menuMethods = menuService.getMenuMethods(role);
		return createMenu(menuMethods);
	}
	
	public ViberKeyBoard createMenu(List<Pair<String, Method>> menuMethods) {
		List<ViberButton> buttons = new ArrayList<>();
		
		for(Pair<String, Method> menuMethod : menuMethods) {
			ViberButton button = createButtonForMethod(menuMethod.getFirst(), menuMethod.getSecond());
			buttons.add(button);
		}
		
		ViberKeyBoard keyboard = ViberKeyBoard
				.builder()
				.Buttons(buttons.toArray(new ViberButton[buttons.size()]))
				.Type(ViberKeyBoardType.keyboard)
				.build();
		return keyboard;
	}

	public ViberButton createButtonForMethod(String beanName, Method method) {
		BotMethod annotation = method.getAnnotation(BotMethod.class);
		ViberButtonActionType type = StringUtils.isEmpty(annotation.url()) ? ViberButtonActionType.reply : ViberButtonActionType.open_url;
		String sActionBody = type == ViberButtonActionType.open_url ? annotation.url() : "";
		
		if(StringUtils.isEmpty(sActionBody)) {
			ViberButtonActionBody actionBody = ViberButtonActionBody.builder().event(Event.clicked).beanName(beanName).methodName(method.getName()).build();
			ObjectMapper mapper = new ObjectMapper();
			try {
				sActionBody = mapper.writeValueAsString(actionBody);
			} catch (JsonProcessingException e) {
				log.error("Error (that never should be)", e);
			}
		}
		
		ViberButton button = ViberButton.builder()
				.ActionType(type)
				.Text(annotation.title())
				.ActionBody(sActionBody)
				.Columns(annotation.cols())
				.Rows(annotation.rows())
				.build();
		return button;
	}
}
