package com.wyc.yandex;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyc.WYCConfig;
import com.wyc.db.model.DriveMessage;
import com.wyc.db.repository.DriveMessageRepository;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
@Slf4j
public class YandexGeoCoderDaemon {

	@Autowired
	private DriveMessageRepository driveMessageRepository;
	
	@Autowired
	private WYCConfig wycConfig;
	
	@Scheduled(fixedRate=10000)
	public void scanLocation() {
		if(wycConfig.isViberDelivery()) {
			List<DriveMessage> messages = driveMessageRepository.findByLocationTitleIsNullAndLongitudeIsNotNull();
			messages.stream().parallel().forEach(dm -> {
				String url = "https://geocode-maps.yandex.ru/1.x/?format=json&geocode=" + dm.getLongitude() + "," + dm.getLatitude();
				OkHttpClient client = new OkHttpClient();
				Request request = new Request.Builder()
					      .url(url)
					      .build();
	
				try {
					Response response = client.newCall(request).execute();
					if(response.isSuccessful()) {
						String body = response.body().string();
						log.info(body);
						ObjectMapper om = new ObjectMapper();
						om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
						YandexGeo yandexGeo = om.readValue(body, YandexGeo.class);
						log.debug("yansexGeo = ", yandexGeo);
						if(yandexGeo.response != null && 
								yandexGeo.response.GeoObjectCollection != null && 
								yandexGeo.response.GeoObjectCollection.featureMember != null && 
								yandexGeo.response.GeoObjectCollection.featureMember.length > 0 
								// && yandexGeo.response.GeoObjectCollection.featureMember[0].GeoObject != null 
								) {
							for(FeatureMember feature : yandexGeo.response.GeoObjectCollection.featureMember) {
								if(feature.GeoObject != null 
										&& feature.GeoObject.metaDataProperty != null 
										&& feature.GeoObject.metaDataProperty.GeocoderMetaData != null 
										&& "street".equalsIgnoreCase(feature.GeoObject.metaDataProperty.GeocoderMetaData.precision)) {
									dm.setLocationTitle(feature.GeoObject.name);
									driveMessageRepository.save(dm);
									break;
								}
							}
						}
					}
				}catch (Exception e) {
					log.error("Error getting " + url, e);
				}
			});
		}
	}
	
	@Data
	@NoArgsConstructor
	public static class YandexGeo {
		private YandexResponse response;
	}
	
	// @Data
	@NoArgsConstructor
	public static class YandexResponse {
		public GeoObjectCollection GeoObjectCollection;
	}
	
	@Data
	@NoArgsConstructor
	public static class GeoObjectCollection {
		private FeatureMember[] featureMember;
	}
	
	public static class FeatureMember {
		public GeoObject GeoObject;
	}
	
	@Data
	@NoArgsConstructor
	public static class GeoObject {
		private String name;
		private GeoMetaData metaDataProperty;
	}
	
	public static class GeoMetaData {
		public GeocoderMetaData GeocoderMetaData;
	}
	
	@Data
	@NoArgsConstructor
	public static class GeocoderMetaData {
		private String precision;
	}
	
}
