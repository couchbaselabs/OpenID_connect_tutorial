package syncgateway.rest.calls;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

public class MyRunnable implements Runnable {

	private static final String SG_PUBLIC_URL = "http://sync-gateway25:4984/";
	private static final String SG_ADMIN_URL = "http://sync-gateway25:4985/";

	private static final String DB = "french_cuisine";

	private static final String BULK_END_POINT = "/_bulk_docs";

	private int offset= 0;
	   public MyRunnable(int offset) {
	       this.offset = offset;
	   }

	   public void run() {
		   
			DocsArray docsArray = new DocsArray();

			for (int i = this.offset * 10; i < 10 + this.offset * 10; i++) {
				DocumentBody docBody = null;
				try {
					docBody = getDocument(i);
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// System.err.println(i);

				docBody.price += 1.;
				// docBody.type = "MON_TYPE_" + docBody.type;
				docBody._revisions.ids.add(0, UUID.randomUUID().toString());
				docBody._revisions.start +=1;
				

				docsArray.addDocumentBody(docBody);
			}

			try {
				updateBulkDocuments(docsArray);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	   }
	   
	   

		/**
		 * http://localhost:4985/french_cuisine/produit_from_CBL_0?revs=true
		 * 
		 * @param id
		 * @throws JsonProcessingException
		 * @throws JsonMappingException
		 */
		private static DocumentBody getDocument(int id) throws JsonMappingException, JsonProcessingException {
			// String encBase64loginPassword = "QWRtaW5pc3RyYXRvcjpwYXNzd29yZA=="; //
			// "Administrator:password";
			// .header("Authorization", "Basic " + encBase64loginPassword)

			HttpResponse<String> response1 = Unirest.get(SG_ADMIN_URL + DB + "/" + "produit_from_CBL_" + id)
					.header("accept", "application/json").queryString("revs", true).asString();

			// System.out.println(response1.getBody().toString());

			return new ObjectMapper().readerFor(DocumentBody.class).readValue(response1.getBody().toString());
		}

		private static void updateBulkDocuments(DocsArray docsArray) throws JsonProcessingException {
			String encBase64loginPassword = "cGF1bDpwYXNzd29yZA=="; // paul:password
					//"QWRtaW5pc3RyYXRvcjpwYXNzd29yZA=="; // "Administrator:password";
			
			
			String docs = new ObjectMapper().writeValueAsString(docsArray);
			
			// System.out.println(docs);

			HttpResponse<String> response1 = Unirest.post(SG_PUBLIC_URL + DB + BULK_END_POINT)
					.header("Authorization", "Basic " + encBase64loginPassword)
					.header("accept", "application/json").header("Content-type", "application/json").body(docs).asString();

			System.out.println("SORTIE : " + response1.getBody().toString());
		}
	}
