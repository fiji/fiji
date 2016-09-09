# -*- mode: ruby -*-
# vi: set ft=ruby :

# This Vagrantfile defines the requirements of a Linux development environment
# to develop Fiji. This environment can be set up conveniently by installing
# Vagrant and VirtualBox and calling "vagrant up" in the Fiji directory.
#
# See https://www.vagrantup.com/ for details

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

$provision = <<PROVISION
apt-get update
apt-get install -y openjdk-8-jdk maven

cat >> /home/vagrant/.profile << \EOF

cd /vagrant
cat << \TOOEOF

Welcome to the Vagrant setup for Fiji!
--------------------------------------

To build Fiji, just execute 'mvn'.
TOOEOF
EOF
PROVISION

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  # Start with a 64-bit Ubuntu 12.04 "Precise Penguin" box
  config.vm.box = "ubuntu"
  config.vm.box_url = "http://files.vagrantup.com/precise64.box"

  config.vm.provision :shell, :inline => $provision
end
