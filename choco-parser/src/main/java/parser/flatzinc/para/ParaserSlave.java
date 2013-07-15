/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Created by IntelliJ IDEA.
 * User: Jean-Guillaume Fages
 * Date: 15/07/13
 * Time: 16:31
 */

package parser.flatzinc.para;

import parser.flatzinc.ParseAndSolve;
import samples.sandbox.parallelism.AbstractParallelMaster;
import samples.sandbox.parallelism.AbstractParallelSlave;
import java.io.IOException;

public class ParaserSlave extends AbstractParallelSlave{

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	ParseAndSolve job;
	String[] args;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public ParaserSlave(AbstractParallelMaster master, int id, String[] args){
		super(master,id);
		this.args = args;
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void work() {
		job = new ParseAndSolve();
		try {
			job.doMain(args);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
