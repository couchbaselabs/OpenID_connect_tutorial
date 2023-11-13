package com.couchbase.tutorial.openid.parser;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ArgumentsParserHelper {

	private static List<String> validChannelsList = Arrays.asList("Bretagne_region", "Alsace_region", "PACA_region");

	public static InputArguments parseArguments(String[] args) throws MissingArgumentException {

		InputArguments input = new InputArguments();

		CommandLine commandLine = null;
		Options options = new Options();
		new Option("help", "print this message");
		options.addOption("u", "user", true, "The user argument. Specify the OIDC user login.");
		options.addOption("p", "password", true, "The password argument. Specify the OIDC user password.");

		options.addOption("ud", "upsert-doc", true,
				"OPTIONAL : if sepcified, value contains the JSON value of the doucment to upsert.");
		options.addOption("dnr", "do-not-replicate", false,
				"OPTIONAL : Enable to true if you do not want to enable replication. Default is false");
		options.addOption("oidc", "oidc", true,
				"OPTIONAL : Enable to false if you do not want to connect using OIDC provider. Default is true");
		CommandLineParser parser = new DefaultParser();

		try {
			commandLine = parser.parse(options, args);
		} catch (ParseException exception) {
			System.out.print("Parse error: ");
			System.out.println(exception.getMessage());
		}
		if (args.length == 0) {
			printHelpCommand(options);
			System.exit(0);
		}

		if (!commandLine.hasOption("--user") && !commandLine.hasOption("-u")) {
			printHelpCommand(options);
			throw new MissingArgumentException("user");
		} else if (!commandLine.hasOption("--password") && !commandLine.hasOption("-p")) {
			printHelpCommand(options);
			throw new MissingArgumentException("password");
		}

		input.setUser(commandLine.getOptionValue("u"));
		input.setPassword(commandLine.getOptionValue("p"));

		// optional arguments
		if (commandLine.hasOption("--upsert-doc") || commandLine.hasOption("-ud")) {
			JsonElement jsonElmt = JsonParser.parseString(commandLine.getOptionValue("ud"));
			input.setJsonDocToUpsert(jsonElmt);

			System.out.println("Option upsert-doc is present.  The value is: " + jsonElmt.toString());
		}

		if (commandLine.hasOption("--do-not-replicate") || commandLine.hasOption("-nr")) {
			input.setDoNotReplicate(true);
		} else {
			input.setDoNotReplicate(false);
		}
		
		if (commandLine.hasOption("--is-oidc") || commandLine.hasOption("-oidc")) {
			boolean isOidc = Boolean.parseBoolean(commandLine.getOptionValue("oidc"));
			input.setOidcUser(isOidc);
		}

		String[] remainder = commandLine.getArgs();
		if (remainder.length > 0) {
			System.out.print("Remaining arguments: ");
			for (String argument : remainder) {
				System.out.print(argument);
				System.out.print(" ");
			}
			System.out.println();
		}

		return input;
	}

	private static void printHelpCommand(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);
		formatter.printHelp("GettingStartedWithOpenIDConnect", options);
	}
}
