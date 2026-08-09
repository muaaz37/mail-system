EXTERNAL_NET = 'any'
HOME_NET = '__HOME_NET__'

-- Load the default Snort 3 configuration.
include '/etc/snort/snort_defaults.lua'

-- Define the HTTP ports forwarded to the WAF.
HTTP_PORTS = '[ __HTTP_PORTS__ ]'

default_variables.ports.HTTP_PORTS = HTTP_PORTS
default_variables.ports.FILE_DATA_PORTS = HTTP_PORTS

-- Configure Snort for inline intrusion prevention.
ips =
{
	enable_builtin_rules = true,
	mode = inline,

	variables = default_variables,
	-- Load the community rules and local rules
	rules = [[
        include /etc/snort/snort3-community-rules/snort3-community.rules
        include /etc/snort/local.rules
    ]]
}

-- Receive forwarded packets from Linux NFQUEUE 1.
daq =
{
	modules =
	{
		{
			name = 'nfq',
			mode = 'inline'
		}
	},

	inputs = { '1' }
}