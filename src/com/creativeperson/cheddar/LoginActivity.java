package com.creativeperson.cheddar;

import net.smartam.leeloo.client.OAuthClient;
import net.smartam.leeloo.client.URLConnectionClient;
import net.smartam.leeloo.client.request.OAuthClientRequest;
import net.smartam.leeloo.client.response.OAuthJSONAccessTokenResponse;
import net.smartam.leeloo.common.exception.OAuthProblemException;
import net.smartam.leeloo.common.exception.OAuthSystemException;
import net.smartam.leeloo.common.message.types.GrantType;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;

import com.creativeperson.cheddar.utility.Constants;

public class LoginActivity extends Activity {
	
	private Button mLoginButton;
	private TextView mLoginText;
	private SharedPreferences mSharedPreferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
		checkAccessToken();
		setContentView(R.layout.activity_login); 
		
		mLoginText = (TextView) findViewById(R.id.login_text);
		Linkify.addLinks(mLoginText, Linkify.ALL);
		
		mLoginButton = (Button)findViewById(R.id.login);
		mLoginButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.signing_in), Toast.LENGTH_SHORT).show();
				OAuthClientRequest request = null;
				try {
					request = OAuthClientRequest
							.authorizationLocation(Constants.AUTHORIZATION_URL)
							.setClientId(Constants.CLIENT_ID).setRedirectURI(Constants.REDIRECT_URI)
							.buildQueryMessage();
				} catch (OAuthSystemException e) {
					e.printStackTrace();
				}

				Intent intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(request.getLocationUri()));
				startActivity(intent);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		Uri uri = getIntent().getData();
		if (uri != null && uri.toString().startsWith(Constants.REDIRECT_URI)) {
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.retrieving_auth_token), Toast.LENGTH_SHORT).show();
			String code = uri.getQueryParameter("code");
			new RetrieveAccessTokenTask().execute(code);
		}
	}
	
	private void checkAccessToken() {
		String accessToken = mSharedPreferences.getString(Constants.ACCESS_TOKEN, null);
		if(accessToken != null) {
			Intent intent = new Intent(this, ListActivity.class);
			startActivity(intent);
		}
	}
	
	private class RetrieveAccessTokenTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String code = params[0];
			if(code != null) {
				OAuthClientRequest request = null;
				String token = null;
				try {
					request = OAuthClientRequest.tokenLocation(Constants.ACCESS_TOKEN_URL)
							.setGrantType(GrantType.AUTHORIZATION_CODE)
							.setClientId(Constants.CLIENT_ID)
							.setClientSecret(Constants.CLIENT_SECRET)
							.setRedirectURI(Constants.REDIRECT_URI)
							.setCode(code)
							.buildBodyMessage();

					OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
					OAuthJSONAccessTokenResponse response = null;
					response = oAuthClient.accessToken(request);
					token = response.getAccessToken();
				} catch (OAuthSystemException e) {
					e.printStackTrace();
				} catch (OAuthProblemException e) {
					e.printStackTrace();
				}
				return token;
			}

			Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_signing_in), Toast.LENGTH_SHORT).show();
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if(result != null) {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.signed_in), Toast.LENGTH_SHORT).show();
				Editor editor = mSharedPreferences.edit();
				editor.putString(Constants.ACCESS_TOKEN, result);
				editor.commit();
				checkAccessToken();
			} else {
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_signing_in), Toast.LENGTH_SHORT).show();
			}
		}
	}
}
