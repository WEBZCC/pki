# Authors:
#     Matthew Harmsen <mharmsen@redhat.com>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 2 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along
# with this program; if not, write to the Free Software Foundation, Inc.,
# 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Copyright (C) 2011 Red Hat, Inc.
# All rights reserved.
#

# System Imports
from __future__ import absolute_import
from __future__ import print_function
import logging
import sys
import signal
import subprocess

if not hasattr(sys, "hexversion") or sys.hexversion < 0x020700f0:
    print("Python version %s.%s.%s is too old." % sys.version_info[:3])
    print("Please upgrade to at least Python 2.7.0.")
    sys.exit(1)
try:
    import os
    import traceback
    import pki
    from pki.server.deployment import pkiconfig as config
    from pki.server.deployment.pkiparser import PKIConfigParser
    from pki.server.deployment import pkilogging
    from pki.server.deployment import pkimessages as log
except ImportError:
    print("""\
There was a problem importing one of the required Python modules. The
error was:

    %s
""" % sys.exc_info()[1], file=sys.stderr)
    sys.exit(1)

logger = logging.LoggerAdapter(
    logging.getLogger('pkidestroy'),
    extra={'indent': ''})

deployer = pki.server.deployment.PKIDeployer()


# Handle the Keyboard Interrupt
# pylint: disable=W0613
def interrupt_handler(event, frame):
    print()
    print('\nUninstallation canceled.')
    sys.exit(1)


# PKI Deployment Functions
def main(argv):
    """main entry point"""

    config.pki_deployment_executable = os.path.basename(argv[0])

    # Set the umask
    os.umask(config.PKI_DEPLOYMENT_DEFAULT_UMASK)

    # Read and process command-line arguments.
    parser = PKIConfigParser(
        'PKI Instance Removal',
        log.PKIDESTROY_EPILOG,
        deployer=deployer)

    parser.optional.add_argument(
        '-i',
        dest='pki_deployed_instance_name',
        action='store',
        nargs=1, metavar='<instance>',
        help='FORMAT:  ${pki_instance_name}')

    parser.optional.add_argument(
        '-u',
        dest='pki_secdomain_user',
        action='store',
        nargs=1, metavar='<security domain user>',
        help='security domain user')

    parser.optional.add_argument(
        '-W',
        dest='pki_secdomain_pass_file',
        action='store',
        nargs=1, metavar='<security domain password file>',
        help='security domain password file path')

    parser.optional.add_argument(
        '--force',
        dest='pki_force_destroy',
        action='store_true',
        help='force removal of subsystem'
    )

    parser.optional.add_argument(
        '--remove-logs',
        dest='pki_remove_logs',
        action='store_true',
        help='remove subsystem logs'
    )

    args = parser.process_command_line_arguments()

    interactive = False

    # Only run this program as "root".
    if not os.geteuid() == 0:
        sys.exit("'%s' must be run as root!" % argv[0])

    while True:

        # -s <subsystem>
        if args.pki_subsystem is None:
            interactive = True
            deployer.subsystem_name = parser.read_text(
                'Subsystem (CA/KRA/OCSP/TKS/TPS)',
                options=['CA', 'KRA', 'OCSP', 'TKS', 'TPS'],
                default='CA', case_sensitive=False).upper()
        else:
            deployer.subsystem_name = str(args.pki_subsystem).strip('[\']')

        # -i <instance name>
        if args.pki_deployed_instance_name is None:
            interactive = True
            config.pki_deployed_instance_name = \
                parser.read_text('Instance', default='pki-tomcat')
        else:
            config.pki_deployed_instance_name = \
                str(args.pki_deployed_instance_name).strip('[\']')

        if interactive:
            print()
            parser.indent = 0

            begin = parser.read_text(
                'Begin uninstallation (Yes/No/Quit)',
                options=['Yes', 'Y', 'No', 'N', 'Quit', 'Q'],
                sign='?', allow_empty=False, case_sensitive=False).lower()

            print()

            if begin == 'q' or begin == 'quit':
                print("Uninstallation canceled.")
                sys.exit(0)

            elif begin == 'y' or begin == 'yes':
                break

        else:
            break

    #    '-u'
    if args.pki_secdomain_user:
        config.pki_secdomain_user = str(args.pki_secdomain_user).strip('[\']')

    #    '-W' password file
    if args.pki_secdomain_pass_file:
        with open(str(args.pki_secdomain_pass_file).strip('[\']'), 'r') as \
                pwd_file:
            config.pki_secdomain_pass = pwd_file.readline().strip('\n')

    #   '--force'
    force_destroy = args.pki_force_destroy

    #   '--remove-logs'
    remove_logs = args.pki_remove_logs

    # verify that previously deployed instance exists
    deployed_pki_instance_path = os.path.join(
        config.PKI_DEPLOYMENT_BASE_ROOT, config.pki_deployed_instance_name
    )
    if not os.path.exists(deployed_pki_instance_path) and not force_destroy:
        print("ERROR:  " + log.PKI_INSTANCE_DOES_NOT_EXIST_1 %
              deployed_pki_instance_path)
        print()
        parser.arg_parser.exit(-1)

    # verify that previously deployed subsystem for this instance exists
    deployed_pki_subsystem_path = os.path.join(
        deployed_pki_instance_path, deployer.subsystem_name.lower()
    )

    if not os.path.exists(deployed_pki_subsystem_path) and not force_destroy:
        print("ERROR:  " + log.PKI_SUBSYSTEM_DOES_NOT_EXIST_2 %
              (deployer.subsystem_name, deployed_pki_instance_path))
        print()
        parser.arg_parser.exit(-1)

    config.default_deployment_cfg = \
        config.PKI_DEPLOYMENT_DEFAULT_CONFIGURATION_FILE

    # establish complete path to previously deployed configuration file
    config.user_deployment_cfg = os.path.join(
        deployed_pki_subsystem_path,
        "registry",
        deployer.subsystem_name.lower(),
        config.USER_DEPLOYMENT_CONFIGURATION
    )

    if force_destroy and not os.path.exists(config.user_deployment_cfg):
        # During force destroy, try to load the file. If file doesn't exist, we ignore it
        config.user_deployment_cfg = None

    parser.validate()
    parser.init_config()

    # Enable 'pkidestroy' logging.
    config.pki_log_dir = config.PKI_DEPLOYMENT_LOG_ROOT
    config.pki_log_name = "pki" + "-" +\
                          deployer.subsystem_name.lower() +\
                          "-" + "destroy" + "." +\
                          deployer.log_timestamp + "." + "log"
    print('Log file: %s/%s' % (config.pki_log_dir, config.pki_log_name))

    pkilogging.enable_pki_logger(config.pki_log_dir,
                                 config.pki_log_name,
                                 config.pki_log_level,
                                 config.pki_console_log_level,
                                 "pkidestroy")

    # Read the specified PKI configuration file.
    rv = parser.read_pki_configuration_file()
    if rv != 0:
        logger.error(log.PKI_UNABLE_TO_PARSE_1, rv)
        sys.exit(1)

    # Combine the various sectional dictionaries into a PKI master dictionary
    parser.compose_pki_master_dictionary()
    parser.mdict['pki_destroy_log'] = \
        config.pki_log_dir + "/" + config.pki_log_name

    # Add force_destroy to master dictionary
    parser.mdict['pki_force_destroy'] = force_destroy

    # Add remove logs to master dictionary
    parser.mdict['pki_remove_logs'] = remove_logs

    logger.debug(log.PKI_DICTIONARY_MASTER)
    logger.debug(pkilogging.log_format(parser.mdict))

    print("Uninstalling " + deployer.subsystem_name + " from " +
          deployed_pki_instance_path + ".")

    # Process the various "scriptlets" to remove the specified PKI subsystem.
    pki_subsystem_scriptlets = parser.mdict['destroy_scriplets'].split()
    deployer.init(parser)

    try:
        for scriptlet_name in pki_subsystem_scriptlets:

            scriptlet_module = __import__(
                "pki.server.deployment.scriptlets." + scriptlet_name,
                fromlist=[scriptlet_name])

            scriptlet = scriptlet_module.PkiScriptlet()

            scriptlet.destroy(deployer)

    except subprocess.CalledProcessError as e:
        log_error_details()
        print()
        print("Uninstallation failed: Command failed: %s" % ' '.join(e.cmd))
        if e.output:
            print(e.output)
        print()
        sys.exit(1)

    except Exception as e:  # pylint: disable=broad-except
        log_error_details()
        print()
        print("Uninstallation failed: %s" % e)
        print()
        sys.exit(1)

    print()
    print("Uninstallation complete.")


def log_error_details():
    e_type, e_value, e_stacktrace = sys.exc_info()
    stacktrace_list = traceback.format_list(traceback.extract_tb(e_stacktrace))
    e_stacktrace = "%s: %s\n" % (e_type.__name__, e_value)
    for l in stacktrace_list:
        e_stacktrace += l
    logger.error(e_stacktrace)
    del e_type, e_value, e_stacktrace


# PKI Deployment Entry Point
if __name__ == "__main__":
    signal.signal(signal.SIGINT, interrupt_handler)
    main(sys.argv)
