# StockHawk
## Udacity project 3: StockHawk

## Summary

Added enhancements to one app in order to make it production ready. The work included ensuring errors were handled gracefully, building a widget for the home screen, Providing a Material theme, adding support for screen readers, optimizations for localization, and data visualization via a library.

## What's it about?

A Stock app that displays the list of active stocks(in quotes) with their bid price and the change percentage or change value.

Users can:-

 - Add a new Stock

- View the stock price by time graph, along with volume of the stocktrade.

- Add a widget on the home screen that displays upto-date info in the form of cards.

>There exists support for talkback and also translation for arabic language which are both untested as of yet.

## Components 
- Each stock quote on the main screen is clickable and leads to a new screen which graphs the stock's value over time.

- Stock Hawk does not crash when a user searches for a non-existent stock.

- Stock Hawk Stocks can be displayed in a collection widget.

- Stock Hawk app has content descriptions for all buttons.

- Stock Hawk app supports layout mirroring using both the RTL attribute and the start/end tags.

- Strings are all included in the strings.xml file and untranslatable strings have a translatable tag marked to false.




![screenshot_2016-06-20-12-01-49](https://cloud.githubusercontent.com/assets/13608668/16203984/7601afa2-373a-11e6-946c-6509462dd764.png)
![screenshot_2016-06-20-12-01-15](https://cloud.githubusercontent.com/assets/13608668/16203985/762f09de-373a-11e6-88d0-1b9dc85c5ac2.png)
![screenshot_2016-06-20-12-00-51](https://cloud.githubusercontent.com/assets/13608668/16203987/7657027c-373a-11e6-9cf0-2007fa74338b.png)




