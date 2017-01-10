#!/usr/bin/env python

import sys
import collections


KEY, PREV, NEXT = range(3)

class OrderedSet(collections.MutableSet):

	def __init__(self, iterable=None):
		self.end = end = [] 
		end += [None, end, end]		# sentinel node for doubly linked list
		self.map = {}			# key --> [key, prev, next]
		if iterable is not None:
			self |= iterable

	def __len__(self):
		return len(self.map)

	def __contains__(self, key):
		return key in self.map

	def add(self, key):
		if key not in self.map:
			end = self.end
			curr = end[PREV]
			curr[NEXT] = end[PREV] = self.map[key] = [key, curr, end]

	def discard(self, key):
		if key in self.map:		
			key, prev, next = self.map.pop(key)
			prev[NEXT] = next
			next[PREV] = prev

	def __iter__(self):
		end = self.end
		curr = end[NEXT]
		while curr is not end:
			yield curr[KEY]
			curr = curr[NEXT]

	def __reversed__(self):
		end = self.end
		curr = end[PREV]
		while curr is not end:
			yield curr[KEY]
			curr = curr[PREV]

	def pop(self, last=True):
		if not self:
			raise KeyError('set is empty')
		key = next(reversed(self)) if last else next(iter(self))
		self.discard(key)
		return key

	def __repr__(self):
		if not self:
			return '%s()' % (self.__class__.__name__,)
		return '%s(%r)' % (self.__class__.__name__, list(self))

	def __eq__(self, other):
		if isinstance(other, OrderedSet):
			return len(self) == len(other) and list(self) == list(other)
		return set(self) == set(other)

	def __del__(self):
		self.clear()			# remove circular references


class MessagesXmlParser():
	def __init__(self, filename):
		import xml.etree.ElementTree as etree
		
		self.keys = []
		
		tree = etree.parse(filename)
		root = tree.getroot()
		for message in root:
			self.keys.append(message.attrib['key'])

class MessagesPropertiesParser():
	def __init__(self, filename):
		try:
			import jprops
		except:
			print('Error: jprops module for parsing .properties files is missing. Download and follow installation instructions from http://mgood.github.com/jprops/')
			sys.exit(2)
		
		self.keys = []
		
		with open(filename) as fp:
			for key, value in jprops.iter_properties(fp):
				self.keys.append(key)

if __name__ == "__main__":
	if len(sys.argv) != 3:
		print("Usage:")
		print("       %s messages.xml messages_XX.xml" % (sys.argv[0]))
		print("or")
		print("       %s Messages.properties Messages_XX.properties" % (sys.argv[0]))
		sys.exit(1)

	testfile = open(sys.argv[1], 'rb')
	if testfile.readline().find('<?xml') != -1:
		# xml file detected, assume messages.xml
		messages_tmpl = MessagesXmlParser(sys.argv[1])
		messages_in   = MessagesXmlParser(sys.argv[2])
	else:
		# assume Messages.properties
		messages_tmpl = MessagesPropertiesParser(sys.argv[1])
		messages_in   = MessagesPropertiesParser(sys.argv[2])
	
	print "Present in %s but missing in %s:" % (sys.argv[1], sys.argv[2])
	for i in OrderedSet(messages_tmpl.keys) - OrderedSet(messages_in.keys):
		print i
	print "\nPresent in %s but missing in %s:" % (sys.argv[2], sys.argv[1])
	for i in OrderedSet(messages_in.keys) - OrderedSet(messages_tmpl.keys):
		print i
