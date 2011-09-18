#!/usr/bin/env ruby

include Math

# Approximate "birth day" probability of a single collision
# occurring in a population of n, with given (hash) space.
def bdp( n, space )
  n = n.to_f
  1 - E ** ( -n * ( n - 1 ) / ( 2 * space ) )
end

# Approximate population capacity before probably of collision p is
# reached with the given (hash) space.
def pop( p, space )
  1.0/2.0 + sqrt( 1.0/4.0 - 2.0 * space * log( 1.0 - p ) )
end

bits = 6*23 #uhash
space = 2**bits

p_urls = pop( ARGV[0].to_f, space )

puts p_urls

puts bdp( p_urls, space )

half = 2**(bits/2)
puts half.to_f
puts bdp( half, space )
