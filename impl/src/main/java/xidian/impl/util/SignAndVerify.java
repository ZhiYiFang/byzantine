/*
 * Copyright © 2017 zhiyifang and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package xidian.impl.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

public class SignAndVerify {
	public static final String SIGN_ALGORITHMS = "SHA1WithRSA";

	public static String sign(String content, String privateKey) {
		byte[] signed = null;
		String str = null;
		try {
			byte[] pckbyte = Base64.decodeBase64(privateKey);

			PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(pckbyte);
			KeyFactory keyf = KeyFactory.getInstance("RSA");
			PrivateKey priKey = keyf.generatePrivate(priPKCS8);
			// sha256
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] sha256Digest = md.digest(content.getBytes());
			// sha1
			java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);
			signature.initSign(priKey);
			signature.update(sha256Digest);
			signed = signature.sign();
			// sign to 16进制字符串
			str = Hex.encodeHexString(signed);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return str;
	}

	public static boolean verify(String content, String sign, String publicKey) {
		try {
			byte[] encodedKey = Base64.decodeBase64(publicKey); // right
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

			// 对数据进行SHA-256签名
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] sha256Digest = md.digest(content.getBytes());
			System.out.println("function doCheck sha256=" + Hex.encodeHexString(sha256Digest));
			// 对数据进行SHA1签名
			java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);

			signature.initVerify(pubKey);
			signature.update(sha256Digest);
			boolean bverify = signature.verify(Hex.decodeHex(sign.toCharArray()));
			return bverify;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void main(String[] args) throws SocketException {

		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface networkInterface = networkInterfaces.nextElement();
			Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
			while (inetAddresses.hasMoreElements()) {
				InetAddress inetAddress = inetAddresses.nextElement();

				System.out.println("ip:" + inetAddress);
			}
		}
	}
}
