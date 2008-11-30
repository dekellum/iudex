#!/opt/bin/jruby

require 'database'

class Url < ActiveRecord::Base
    set_primary_key :gcid
end


url = Url.new do |u|
  u.gcid = "38a"
  u.url = "http://gravitext.com/38a"
end
url.save!

# url = nil

Url.find(:all).each do |u|
  puts u.gcid
end

