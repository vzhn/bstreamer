#!/bin/sh
getent group %{mediaserver_group} >/dev/null || groupadd -r %{mediaserver_group}
getent passwd %{mediaserver_user} >/dev/null || /usr/sbin/useradd --comment "Mediaserver" --shell /sbin/nologin -M -r -g %{mediaserver_group} --home %{mediaserver_home} %{mediaserver_user}
