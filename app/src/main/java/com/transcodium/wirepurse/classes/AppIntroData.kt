/**
# Copyright 2018 - Transcodium Ltd.
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the  Apache License v2.0 which accompanies this distribution.
#
#  The Apache License v2.0 is available at
#  http://www.opensource.org/licenses/apache2.0.php
#
#  You are required to redistribute this code under the same licenses.
#
#  Project TNSMoney
#  @author Razak Zakari <razak@transcodium.com>
#  https://transcodium.com
#  created_at 26/07/2018
 **/

package com.transcodium.wirepurse.classes

data class AppIntroData(
        val title: Int,
        val desc: Int,
        val image: Int,
        val bgColor: Int,
        val titleColor: Int? = null)