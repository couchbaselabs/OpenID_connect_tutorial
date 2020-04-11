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

public class ArgumentsParserHelper {

	private static List<String> validChannelsList = Arrays.asList("PDV_Bretagne", "PDV_Alsace_role", "PDV_PACA_role");

	public static InputArguments parseArguments(String[] args) throws MissingArgumentException {

		InputArguments input = new InputArguments();

		CommandLine commandLine = null;
		Options options = new Options();
		new Option("help", "print this message");
		options.addOption("u", "user", true, "The user argument. Specify the OIDC user login.");
		options.addOption("p", "password", true, "The password argument. Specify the OIDC user password.");

		options.addOption("d", "create-doc", true,
				"OPTIONAL : the create-doc argument. Specify the number of new documents to create in the local database. If specified, it MUST be >=1.");
		options.addOption("c", "channel", true,
				"OPTIONAL : the channel argument. Must be present if '--create-doc' option is enabled.");

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
		if (commandLine.hasOption("--create-doc") || commandLine.hasOption("-d")) {
			input.setNumberNewDocsToCreate(Integer.parseInt(commandLine.getOptionValue("d")));
			if (input.getNumberNewDocsToCreate() < 1) {
				throw new IllegalArgumentException("If specified, '--create-doc' MUST be >=1. Current value is "
						+ input.getNumberNewDocsToCreate());
			}
			System.out.println("Option create-doc is present.  The value is: " + input.getNumberNewDocsToCreate());

			if (commandLine.hasOption("--channel") || commandLine.hasOption("-c")) {
				input.setChannelValue(commandLine.getOptionValue("c"));

				if (!validChannelsList.contains(input.getChannelValue())) {
					throw new IllegalArgumentException(
							"If specified, '--channel' value MUST be 'PDV_Bretagne' or 'PDV_Alsace_role' or 'PDV_PACA_role'. Current value is "
									+ input.getChannelValue());
				}

				System.out.println("Option channel is present.  The value is: " + input.getChannelValue());
			} else {
				System.err.println("'channel' option is mandatory if 'create-doc' option is enabled.");
				System.exit(-1);
			}
		}

		String[] remainder = commandLine.getArgs();
		System.out.print("Remaining arguments: ");
		for (String argument : remainder) {
			System.out.print(argument);
			System.out.print(" ");
		}
		System.out.println();

		return input;
	}

	private static void printHelpCommand(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);
		formatter.printHelp("GettingStartedWithOpenIDConnect", options);
	}
}
